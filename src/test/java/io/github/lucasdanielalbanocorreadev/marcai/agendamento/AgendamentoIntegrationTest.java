package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.lucasdanielalbanocorreadev.marcai.TestcontainersConfig;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.AgendamentoResponse;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.ClienteRepository;
import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import io.github.lucasdanielalbanocorreadev.marcai.servico.ServicoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.HorarioIndisponivelException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AgendamentoIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AgendamentoService service;
    @Autowired
    ClienteRepository clientes;
    @Autowired
    ServicoRepository servicos;
    @Autowired
    JdbcTemplate jdbc;

    @Value("${oauth.client.id}")
    String clientId;
    @Value("${oauth.client.secret}")
    String clientSecret;

    Cliente cliente;
    Servico servico;
    /** Além da janela de 24h, para o agendador de lembretes não pegar dados de teste. */
    OffsetDateTime inicio = OffsetDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS);

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE agendamento, cliente, servico, evento_processado RESTART IDENTITY CASCADE");
        cliente = clientes.save(new Cliente("Ana", "5511999998888"));
        servico = servicos.save(new Servico("Corte", 60, new BigDecimal("50.00")));
    }

    @Test
    void horarioSobrepostoEhRecusadoPeloBanco() {
        criar(inicio);

        assertThatThrownBy(() -> criar(inicio.plusMinutes(30)))
                .isInstanceOf(HorarioIndisponivelException.class);
    }

    @Test
    void agendamentoQueComecaQuandoOAnteriorTerminaEhPermitido() {
        criar(inicio);

        assertThat(criar(inicio.plusMinutes(60)).status()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void cancelarLiberaOHorario() {
        AgendamentoResponse primeiro = criar(inicio);
        service.cancelar(primeiro.id());

        assertThat(criar(inicio).id()).isNotEqualTo(primeiro.id());
    }

    @Test
    void apiSemTokenResponde401() throws Exception {
        mvc.perform(get("/api/agendamentos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void fluxoHttpComTokenClientCredentials() throws Exception {
        String resposta = mvc.perform(post("/oauth2/token")
                        .with(httpBasic(clientId, clientSecret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + JsonPath.read(resposta, "$.access_token");
        String corpo = """
                {"clienteId": %d, "servicoId": %d, "inicio": "%s"}
                """.formatted(cliente.getId(), servico.getId(), inicio);

        mvc.perform(post("/api/agendamentos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.clienteNome").value("Ana"));

        mvc.perform(post("/api/agendamentos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um agendamento nesse horário"));
    }

    private AgendamentoResponse criar(OffsetDateTime quando) {
        return service.criar(new CriarAgendamentoRequest(cliente.getId(), servico.getId(), quando));
    }
}
