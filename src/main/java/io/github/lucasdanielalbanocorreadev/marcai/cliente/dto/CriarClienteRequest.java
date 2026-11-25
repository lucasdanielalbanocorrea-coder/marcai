package io.github.lucasdanielalbanocorreadev.marcai.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        /** E.164 sem o "+", ex: 5511999998888 — o mesmo formato que a Meta usa. */
        @NotBlank @Pattern(regexp = "\\d{10,15}", message = "deve conter apenas dígitos com DDI e DDD") String telefone) {
}
