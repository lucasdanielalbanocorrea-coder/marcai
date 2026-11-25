package io.github.lucasdanielalbanocorreadev.marcai.whatsapp;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Envia mensagens pela WhatsApp Cloud API. Fora da janela de 24h após a última mensagem
 * do cliente, a Meta só aceita templates aprovados, por isso os lembretes usam template.
 */
@Component
@RequiredArgsConstructor
public class WhatsAppClient {

    private final RestClient whatsAppRestClient;
    private final WhatsAppProperties properties;

    /** Envia o template de lembrete preenchendo as variáveis {{1}}, {{2}}... do corpo, na ordem. */
    public void enviarLembrete(String telefone, List<String> parametros) {
        var corpo = Map.of(
                "messaging_product", "whatsapp",
                "to", telefone,
                "type", "template",
                "template", Map.of(
                        "name", properties.templateLembrete(),
                        "language", Map.of("code", properties.idiomaTemplate()),
                        "components", List.of(Map.of(
                                "type", "body",
                                "parameters", parametros.stream()
                                        .map(texto -> Map.of("type", "text", "text", texto))
                                        .toList()))));

        whatsAppRestClient.post()
                .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
    }
}
