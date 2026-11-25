package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook;

import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppProperties;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook.dto.WebhookPayload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Endpoint público (sem token OAuth): a autenticidade vem da assinatura HMAC da Meta.
 * Configure no painel da Meta a URL https://SEU_HOST/webhook/whatsapp.
 */
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WebhookController {

    /** Os eventos da Meta têm poucos KB; 1 MB deixa folga para lotes grandes. */
    static final int TAMANHO_MAXIMO_CORPO = 1024 * 1024;

    private final WhatsAppProperties properties;
    private final AssinaturaValidator assinaturaValidator;
    private final WebhookService service;
    private final JsonMapper jsonMapper;

    /** Handshake feito pela Meta ao cadastrar o webhook. */
    @GetMapping
    ResponseEntity<String> verificar(
            @RequestParam("hub.mode") String modo,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String desafio) {
        boolean tokenValido = MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                properties.verifyToken().getBytes(StandardCharsets.UTF_8));
        if ("subscribe".equals(modo) && tokenValido) {
            return ResponseEntity.ok(desafio);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * O corpo é lido como bytes porque a assinatura é calculada sobre o conteúdo bruto, e com limite
     * de tamanho porque a leitura acontece antes de saber se a requisição é mesmo da Meta: sem ele,
     * qualquer um esgotaria a memória enviando um corpo gigante (inclusive chunked, sem Content-Length).
     */
    @PostMapping
    ResponseEntity<Void> receber(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura,
            InputStream entrada) throws IOException {
        byte[] corpo = entrada.readNBytes(TAMANHO_MAXIMO_CORPO + 1);
        if (corpo.length > TAMANHO_MAXIMO_CORPO) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        }
        if (!assinaturaValidator.valida(corpo, assinatura)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.processar(jsonMapper.readValue(corpo, WebhookPayload.class));
        return ResponseEntity.ok().build();
    }
}
