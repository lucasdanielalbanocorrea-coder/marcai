package io.github.lucasdanielalbanocorreadev.marcai.servico;

import io.github.lucasdanielalbanocorreadev.marcai.servico.dto.CriarServicoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.servico.dto.ServicoResponse;
import io.github.lucasdanielalbanocorreadev.marcai.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicoService {

    private final ServicoRepository repository;

    @Transactional
    public ServicoResponse criar(CriarServicoRequest request) {
        return ServicoResponse.de(repository.save(
                new Servico(request.nome(), request.duracaoMinutos(), request.preco())));
    }

    public List<ServicoResponse> listar() {
        return repository.findAll().stream().map(ServicoResponse::de).toList();
    }

    public ServicoResponse buscar(Long id) {
        return ServicoResponse.de(buscarEntidade(id));
    }

    public Servico buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }
}
