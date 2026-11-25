package io.github.lucasdanielalbanocorreadev.marcai.servico.dto;

import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import java.math.BigDecimal;

public record ServicoResponse(Long id, String nome, int duracaoMinutos, BigDecimal preco) {

    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(servico.getId(), servico.getNome(), servico.getDuracaoMinutos(), servico.getPreco());
    }
}
