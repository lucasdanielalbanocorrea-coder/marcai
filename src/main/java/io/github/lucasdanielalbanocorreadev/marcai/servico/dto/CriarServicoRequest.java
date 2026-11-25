package io.github.lucasdanielalbanocorreadev.marcai.servico.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarServicoRequest(
        @NotBlank @Size(max = 120) String nome,
        @Positive @Max(24 * 60) int duracaoMinutos,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal preco) {
}
