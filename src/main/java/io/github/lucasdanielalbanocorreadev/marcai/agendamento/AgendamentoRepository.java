package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @EntityGraph(attributePaths = {"cliente", "servico"})
    List<Agendamento> findByInicioGreaterThanEqualAndInicioLessThanOrderByInicio(OffsetDateTime de, OffsetDateTime ate);

    @Query("""
            select a from Agendamento a
            join fetch a.cliente
            join fetch a.servico
            where a.status = io.github.lucasdanielalbanocorreadev.marcai.agendamento.StatusAgendamento.AGENDADO
              and a.lembreteEnviadoEm is null
              and a.inicio > :agora
              and a.inicio <= :limite
            order by a.inicio
            """)
    List<Agendamento> pendentesDeLembrete(OffsetDateTime agora, OffsetDateTime limite);

    /** O agendamento a que o cliente está respondendo: o próximo que já recebeu lembrete. */
    Optional<Agendamento> findFirstByClienteTelefoneAndStatusInAndLembreteEnviadoEmIsNotNullAndInicioAfterOrderByInicio(
            String telefone, Collection<StatusAgendamento> status, OffsetDateTime agora);
}
