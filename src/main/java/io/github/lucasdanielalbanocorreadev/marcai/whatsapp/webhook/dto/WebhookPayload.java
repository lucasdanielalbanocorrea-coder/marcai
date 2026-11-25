package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Stream;

/**
 * Subconjunto do payload que a Meta envia para o campo "messages". Campos desconhecidos são ignorados.
 * Referência: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components
 */
public record WebhookPayload(List<Entry> entry) {

    public Stream<Mensagem> mensagens() {
        return Stream.ofNullable(entry).flatMap(List::stream)
                .flatMap(e -> Stream.ofNullable(e.changes()).flatMap(List::stream))
                .filter(c -> c.value() != null)
                .flatMap(c -> Stream.ofNullable(c.value().messages()).flatMap(List::stream));
    }

    public record Entry(List<Change> changes) {
    }

    public record Change(String field, Value value) {
    }

    /** Também chegam "statuses" (entregue, lido...) neste objeto; não são usados. */
    public record Value(List<Mensagem> messages) {
    }

    /**
     * @param from telefone do cliente em E.164 sem o "+"
     * @param id   wamid, único por mensagem — usado para idempotência
     */
    public record Mensagem(String from, String id, String type, Texto text, Botao button, Interativo interactive) {

        /** O que o cliente escreveu ou o botão que tocou, conforme o tipo da mensagem. */
        public String conteudo() {
            if (text != null) {
                return text.body();
            }
            if (button != null) {
                return button.payload() != null ? button.payload() : button.text();
            }
            if (interactive != null && interactive.buttonReply() != null) {
                return interactive.buttonReply().id();
            }
            return null;
        }
    }

    public record Texto(String body) {
    }

    /** Resposta a um botão de resposta rápida de template. */
    public record Botao(String payload, String text) {
    }

    public record Interativo(@JsonProperty("button_reply") BotaoResposta buttonReply) {
    }

    public record BotaoResposta(String id, String title) {
    }
}
