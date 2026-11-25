package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucasdanielalbanocorreadev.marcai.TestcontainersConfig;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoService;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.StatusAgendamento;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.ClienteRepository;
import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import io.github.lucasdanielalbanocorreadev.marcai.servico.ServicoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.EventoProcessadoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class WebhookControllerTest {

    private static final String TELEFONE = "5511999998888";

    @Autowired
    MockMvc mvc;
    @Autowired
    WhatsAppProperties properties;
    @Autowired
    AgendamentoService agendamentoService;
    @Autowired
    AgendamentoRepository agendamentos;
    @Autowired
    ClienteRepository clientes;
    @Autowired
    ServicoRepository servicos;
    @Autowired
    EventoProcessadoRepository eventos;
    @Autowired
    JdbcTemplate jdbc;

    Long agendamentoId;

    /** Um agendamento cujo lembrete já foi enviado, aguardando a resposta do cliente. */
    @BeforeEach
    void prepararAgendamentoAguardandoResposta() {
        jdbc.execute("TRUNCATE agendamento, cliente, servico, evento_processado RESTART IDENTITY CASCADE");
        Cliente cliente = clientes.save(new Cliente("Ana", TELEFONE));
        Servico servico = servicos.save(new Servico("Corte", 60, new BigDecimal("50.00")));
        agendamentoId = agendamentoService.criar(new CriarAgendamentoRequest(cliente.getId(), servico.getId(),
                OffsetDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS))).id();
        agendamentoService.marcarLembreteEnviado(agendamentoId);
    }

    @Test
    void handshakeComVerifyTokenCorretoDevolveODesafio() throws Exception {
        mvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", properties.verifyToken())
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isOk())
                .andExpect(content().string("1158201444"));
    }

    @Test
    void handshakeComVerifyTokenErradoEhRecusado() throws Exception {
        mvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "errado")
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mensagemSemAssinaturaOuComAssinaturaInvalidaEhRecusada() throws Exception {
        String corpo = mensagemDeTexto("wamid.1", "2");

        mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
                        .header("X-Hub-Signature-256", "sha256=" + "00".repeat(32)))
                .andExpect(status().isUnauthorized());

        assertThat(agendamentos.findById(agendamentoId).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void corpoAcimaDoLimiteEhRecusadoAntesDeValidarAAssinatura() throws Exception {
        byte[] gigante = new byte[WebhookController.TAMANHO_MAXIMO_CORPO + 1];

        mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(gigante)
                        .header("X-Hub-Signature-256", "sha256=" + "00".repeat(32)))
                .andExpect(status().isContentTooLarge());
    }

    @Test
    void respostaDeCancelamentoEhAplicadaUmaUnicaVezMesmoSeAMetaReenviar() throws Exception {
        String corpo = mensagemDeTexto("wamid.1", "Não");

        enviarAssinado(corpo).andExpect(status().isOk());
        enviarAssinado(corpo).andExpect(status().isOk());

        assertThat(agendamentos.findById(agendamentoId).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(eventos.count()).isEqualTo(1);
    }

    @Test
    void botaoDeRespostaRapidaConfirmaOAgendamento() throws Exception {
        String corpo = """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
                  "messaging_product":"whatsapp",
                  "messages":[{"from":"%s","id":"wamid.2","timestamp":"1700000000","type":"button",
                               "button":{"payload":"CONFIRMAR","text":"Confirmar"}}]}}]}]}
                """.formatted(TELEFONE);

        enviarAssinado(corpo).andExpect(status().isOk());

        assertThat(agendamentos.findById(agendamentoId).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    private ResultActions enviarAssinado(String corpo) throws Exception {
        return WebhookMeta.entregarAssinado(mvc, properties.appSecret(), corpo);
    }

    private static String mensagemDeTexto(String id, String texto) {
        return WebhookMeta.mensagemDeTexto(TELEFONE, id, texto);
    }
}
