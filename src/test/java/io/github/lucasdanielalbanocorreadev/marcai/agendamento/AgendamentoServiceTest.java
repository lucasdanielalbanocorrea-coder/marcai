package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.AgendamentoResponse;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.ClienteService;
import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import io.github.lucasdanielalbanocorreadev.marcai.servico.ServicoService;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.HorarioIndisponivelException;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final String TELEFONE = "5511999998888";
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2030-01-10T14:00:00-03:00");

    @Mock
    AgendamentoRepository repository;
    @Mock
    ClienteService clienteService;
    @Mock
    ServicoService servicoService;
    @InjectMocks
    AgendamentoService service;

    private final Cliente cliente = new Cliente("Ana", TELEFONE);
    private final Servico corte = new Servico("Corte", 45, new BigDecimal("50.00"));

    @Test
    void criarCalculaOFimPelaDuracaoDoServico() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(servicoService.buscarEntidade(2L)).thenReturn(corte);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        AgendamentoResponse resposta = service.criar(new CriarAgendamentoRequest(1L, 2L, INICIO));

        assertThat(resposta.fim()).isEqualTo(INICIO.plusMinutes(45));
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void criarComClienteInexistenteNaoSalvaNada() {
        when(clienteService.buscarEntidade(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente", 99L));

        assertThatThrownBy(() -> service.criar(new CriarAgendamentoRequest(99L, 2L, INICIO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void violacaoDaConstraintDeSobreposicaoViraHorarioIndisponivel() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(servicoService.buscarEntidade(2L)).thenReturn(corte);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("conflito",
                new SQLException("conflito", AgendamentoService.EXCLUSION_VIOLATION)));

        assertThatThrownBy(() -> service.criar(new CriarAgendamentoRequest(1L, 2L, INICIO)))
                .isInstanceOf(HorarioIndisponivelException.class);
    }

    @Test
    void outrasViolacoesDeIntegridadeNaoSaoMascaradas() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(servicoService.buscarEntidade(2L)).thenReturn(corte);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("outra",
                new SQLException("violates foreign key constraint", "23503")));

        assertThatThrownBy(() -> service.criar(new CriarAgendamentoRequest(1L, 2L, INICIO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void respostaNegativaAoLembreteCancelaOProximoAgendamento() {
        Agendamento agendamento = new Agendamento(cliente, corte, INICIO);
        when(repository.findFirstByClienteTelefoneAndStatusInAndLembreteEnviadoEmIsNotNullAndInicioAfterOrderByInicio(
                eq(TELEFONE), any(), any())).thenReturn(Optional.of(agendamento));

        assertThat(service.registrarRespostaAoLembrete(TELEFONE, false)).isTrue();
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void respostaSemAgendamentoAguardandoNaoFazNada() {
        when(repository.findFirstByClienteTelefoneAndStatusInAndLembreteEnviadoEmIsNotNullAndInicioAfterOrderByInicio(
                eq(TELEFONE), any(), any())).thenReturn(Optional.empty());

        assertThat(service.registrarRespostaAoLembrete(TELEFONE, true)).isFalse();
    }
}
