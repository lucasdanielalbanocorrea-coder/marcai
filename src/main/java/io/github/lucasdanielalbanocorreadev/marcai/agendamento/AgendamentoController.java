package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.AgendamentoResponse;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.dto.CriarAgendamentoRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;

    @PostMapping
    ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody CriarAgendamentoRequest request) {
        AgendamentoResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/agendamentos/" + criado.id())).body(criado);
    }

    /** Agendamentos com início em [de, ate). */
    @GetMapping
    List<AgendamentoResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return service.listar(de, ate);
    }

    @GetMapping("/{id}")
    AgendamentoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/cancelamento")
    AgendamentoResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
