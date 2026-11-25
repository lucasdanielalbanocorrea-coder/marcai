package io.github.lucasdanielalbanocorreadev.marcai.lembrete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.lucasdanielalbanocorreadev.marcai.TestcontainersConfig;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.Agendamento;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoService;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.StatusAgendamento;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.ClienteRepository;
import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import io.github.lucasdanielalbanocorreadev.marcai.servico.ServicoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppProperties;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook.WebhookMeta;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Jornada completa sem o WhatsApp real: a Graph API da Meta é substituída por um servidor HTTP
 * local que registra o que a aplicação envia, e a resposta do cliente chega pelo webhook assinado.
 * O agendador automático fica desligado na prática (intervalo de 1h); os testes o chamam direto.
 */
@SpringBootTest(properties = "lembrete.intervalo=PT1H")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class LembreteFluxoCompletoTest {

    private static final String TELEFONE = "5511999998888";

    record Requisicao(String caminho, String autorizacao, String corpo) {
    }

    private static final List<Requisicao> recebidas = new CopyOnWriteArrayList<>();
    private static volatile int statusDaGraphApi = 200;
    private static final HttpServer graphApi = iniciarGraphApiFalsa();

    private static HttpServer iniciarGraphApiFalsa() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/", troca -> {
                recebidas.add(new Requisicao(
                        troca.getRequestURI().getPath(),
                        troca.getRequestHeaders().getFirst("Authorization"),
                        new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                byte[] resposta = "{\"messages\":[{\"id\":\"wamid.fake\"}]}".getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(statusDaGraphApi, resposta.length);
                troca.getResponseBody().write(resposta);
                troca.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @DynamicPropertySource
    static void apontarParaGraphApiFalsa(DynamicPropertyRegistry registry) {
        registry.add("whatsapp.api-url", () -> "http://localhost:" + graphApi.getAddress().getPort() + "/v23.0");
    }

    @AfterAll
    static void pararGraphApiFalsa() {
        graphApi.stop(0);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    LembreteScheduler scheduler;
    @Autowired
    AgendamentoService agendamentoService;
    @Autowired
    AgendamentoRepository agendamentos;
    @Autowired
    ClienteRepository clientes;
    @Autowired
    ServicoRepository servicos;
    @Autowired
    WhatsAppProperties whatsApp;
    @Autowired
    JdbcTemplate jdbc;

    OffsetDateTime inicio = OffsetDateTime.now().plusHours(2).truncatedTo(ChronoUnit.MINUTES);
    Long agendamentoId;

    @BeforeEach
    void agendarDaquiADuasHoras() {
        jdbc.execute("TRUNCATE agendamento, cliente, servico, evento_processado RESTART IDENTITY CASCADE");
        recebidas.clear();
        statusDaGraphApi = 200;
        Cliente cliente = clientes.save(new Cliente("Ana", TELEFONE));
        Servico servico = servicos.save(new Servico("Corte", 60, new BigDecimal("50.00")));
        agendamentoId = agendamentoService.criar(
                new CriarAgendamentoRequest(cliente.getId(), servico.getId(), inicio)).id();
    }

    @Test
    void lembreteEnviadoERespostaDoClienteConfirmaOAgendamento() throws Exception {
        scheduler.enviarLembretes();

        assertThat(recebidas).hasSize(1);
        Requisicao envio = recebidas.getFirst();
        assertThat(envio.caminho()).isEqualTo("/v23.0/" + whatsApp.phoneNumberId() + "/messages");
        assertThat(envio.autorizacao()).isEqualTo("Bearer " + whatsApp.token());
        assertThat(JsonPath.<String>read(envio.corpo(), "$.to")).isEqualTo(TELEFONE);
        assertThat(JsonPath.<String>read(envio.corpo(), "$.template.name")).isEqualTo(whatsApp.templateLembrete());
        assertThat(JsonPath.<List<String>>read(envio.corpo(), "$.template.components[0].parameters[*].text"))
                .containsExactly("Ana", "Corte", inicio.atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")));

        scheduler.enviarLembretes();
        assertThat(recebidas).as("o lembrete não é reenviado").hasSize(1);

        WebhookMeta.entregarAssinado(mvc, whatsApp.appSecret(), WebhookMeta.mensagemDeTexto(TELEFONE, "wamid.r1", "1"))
                .andExpect(status().isOk());

        assertThat(agendamento().getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void falhaNaMetaNaoMarcaOLembreteComoEnviadoETentaDeNovo() {
        statusDaGraphApi = 500;
        scheduler.enviarLembretes();
        assertThat(agendamento().getLembreteEnviadoEm()).isNull();

        statusDaGraphApi = 200;
        scheduler.enviarLembretes();
        assertThat(agendamento().getLembreteEnviadoEm()).isNotNull();
        assertThat(recebidas).hasSize(2);
    }

    private Agendamento agendamento() {
        return agendamentos.findById(agendamentoId).orElseThrow();
    }
}
