package io.github.lucasdanielalbanocorreadev.marcai.cliente;

import io.github.lucasdanielalbanocorreadev.marcai.cliente.dto.ClienteResponse;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.dto.CriarClienteRequest;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository repository;

    @Transactional
    public ClienteResponse criar(CriarClienteRequest request) {
        return ClienteResponse.de(repository.save(new Cliente(request.nome(), request.telefone())));
    }

    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(ClienteResponse::de).toList();
    }

    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(buscarEntidade(id));
    }

    public Cliente buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }
}
