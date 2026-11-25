package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.AgendamentoResponse;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.ClienteService;
import io.github.lucasdanielalbanocorreadev.marcai.servico.ServicoService;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.HorarioIndisponivelException;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.RecursoNaoEncontradoException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendamentoService {

    /**
     * SQLState do Postgres para violação de constraint de exclusão. A única na tabela é
     * agendamento_sem_sobreposicao (V3__create_agendamento.sql); o Hibernate não extrai o
     * nome desse tipo de constraint, por isso a identificação é pelo código.
     */
    static final String EXCLUSION_VIOLATION = "23P01";

    private static final Set<StatusAgendamento> ATIVOS = Set.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);

    private final AgendamentoRepository repository;
    private final ClienteService clienteService;
    private final ServicoService servicoService;

    /**
     * A disponibilidade é garantida pela constraint do banco (à prova de concorrência);
     * aqui só traduzimos a violação para um erro de negócio.
     */
    @Transactional
    public AgendamentoResponse criar(CriarAgendamentoRequest request) {
        Agendamento agendamento = new Agendamento(
                clienteService.buscarEntidade(request.clienteId()),
                servicoService.buscarEntidade(request.servicoId()),
                request.inicio());
        try {
            return AgendamentoResponse.de(repository.saveAndFlush(agendamento));
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof SQLException sql
                    && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                throw new HorarioIndisponivelException();
            }
            throw ex;
        }
    }

    public AgendamentoResponse buscar(Long id) {
        return AgendamentoResponse.de(buscarEntidade(id));
    }

    public List<AgendamentoResponse> listar(OffsetDateTime de, OffsetDateTime ate) {
        return repository.findByInicioGreaterThanEqualAndInicioLessThanOrderByInicio(de, ate).stream()
                .map(AgendamentoResponse::de)
                .toList();
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        agendamento.cancelar();
        return AgendamentoResponse.de(agendamento);
    }

    /** Agendamentos que começam até {@code limite} e ainda não receberam lembrete (cliente e serviço já carregados). */
    public List<Agendamento> pendentesDeLembrete(OffsetDateTime limite) {
        return repository.pendentesDeLembrete(OffsetDateTime.now(), limite);
    }

    @Transactional
    public void marcarLembreteEnviado(Long id) {
        buscarEntidade(id).marcarLembreteEnviado(OffsetDateTime.now());
    }

    /**
     * Aplica a resposta do cliente ao lembrete no próximo agendamento dele.
     *
     * @return {@code false} se não há agendamento aguardando resposta para esse telefone
     */
    @Transactional
    public boolean registrarRespostaAoLembrete(String telefone, boolean confirmou) {
        return repository
                .findFirstByClienteTelefoneAndStatusInAndLembreteEnviadoEmIsNotNullAndInicioAfterOrderByInicio(
                        telefone, ATIVOS, OffsetDateTime.now())
                .map(agendamento -> {
                    if (confirmou) {
                        agendamento.confirmar();
                    } else {
                        agendamento.cancelar();
                    }
                    return true;
                })
                .orElse(false);
    }

    private Agendamento buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }
}
