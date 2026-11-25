package io.github.lucasdanielalbanocorreadev.marcai.cliente.dto;

import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;

public record ClienteResponse(Long id, String nome, String telefone) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone());
    }
}
