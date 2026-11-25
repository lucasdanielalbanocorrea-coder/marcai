package io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CriarAgendamentoRequest(
        @NotNull Long clienteId,
        @NotNull Long servicoId,
        /** ISO-8601 com fuso, ex: 2026-12-01T14:30:00-03:00 */
        @NotNull @Future OffsetDateTime inicio) {
}
