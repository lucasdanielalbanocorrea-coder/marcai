package io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.Agendamento;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.StatusAgendamento;
import java.time.OffsetDateTime;

public record AgendamentoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        Long servicoId,
        String servicoNome,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        StatusAgendamento status) {

    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(
                a.getId(),
                a.getCliente().getId(),
                a.getCliente().getNome(),
                a.getServico().getId(),
                a.getServico().getNome(),
                a.getInicio(),
                a.getFim(),
                a.getStatus());
    }
}
