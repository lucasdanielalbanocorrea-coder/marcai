package io.github.lucasdanielalbanocorreadev.marcai.servico;

import io.github.lucasdanielalbanocorreadev.marcai.servico.dto.CriarServicoRequest;
import io.github.lucasdanielalbanocorreadev.marcai.servico.dto.ServicoResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;

    @PostMapping
    ResponseEntity<ServicoResponse> criar(@Valid @RequestBody CriarServicoRequest request) {
        ServicoResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/servicos/" + criado.id())).body(criado);
    }

    @GetMapping
    List<ServicoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    ServicoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
