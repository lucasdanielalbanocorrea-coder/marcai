package io.github.lucasdanielalbanocorreadev.marcai.cliente;

import io.github.lucasdanielalbanocorreadev.marcai.cliente.dto.ClienteResponse;
import io.github.lucasdanielalbanocorreadev.marcai.cliente.dto.CriarClienteRequest;
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
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    ResponseEntity<ClienteResponse> criar(@Valid @RequestBody CriarClienteRequest request) {
        ClienteResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + criado.id())).body(criado);
    }

    @GetMapping
    List<ClienteResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    ClienteResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
