package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Imita a Meta nos testes: monta payloads do webhook e os entrega assinados, como em produção. */
public final class WebhookMeta {

    private WebhookMeta() {
    }

    public static String mensagemDeTexto(String telefone, String id, String texto) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
                  "messaging_product":"whatsapp",
                  "messages":[{"from":"%s","id":"%s","timestamp":"1700000000","type":"text","text":{"body":"%s"}}]}}]}]}
                """.formatted(telefone, id, texto);
    }

    public static ResultActions entregarAssinado(MockMvc mvc, String appSecret, String corpo) throws Exception {
        return mvc.perform(post("/webhook/whatsapp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", assinar(appSecret, corpo))
                .content(corpo));
    }

    static String assinar(String appSecret, String corpo) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
    }
}
