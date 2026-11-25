package io.github.lucasdanielalbanocorreadev.marcai.whatsapp;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais da WhatsApp Cloud API (Meta). Sem valor padrão: a aplicação não sobe sem elas.
 *
 * @param apiUrl            base da Graph API, ex: https://graph.facebook.com/v23.0
 * @param token             token de acesso (System User) usado para enviar mensagens
 * @param phoneNumberId     id do número remetente
 * @param appSecret         segredo do app, usado para validar o header X-Hub-Signature-256
 * @param verifyToken       token que você define no painel da Meta para o handshake do webhook
 * @param templateLembrete  nome do template aprovado usado nos lembretes
 * @param idiomaTemplate    código de idioma do template, ex: pt_BR
 */
@Validated
@ConfigurationProperties("whatsapp")
public record WhatsAppProperties(
        @NotBlank String apiUrl,
        @NotBlank String token,
        @NotBlank String phoneNumberId,
        @NotBlank String appSecret,
        @NotBlank String verifyToken,
        @NotBlank String templateLembrete,
        @NotBlank String idiomaTemplate) {
}
