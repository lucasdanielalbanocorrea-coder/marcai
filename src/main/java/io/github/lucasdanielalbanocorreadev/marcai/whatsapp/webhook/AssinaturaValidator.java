package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook;

import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Valida o header X-Hub-Signature-256 que a Meta envia: "sha256=" + HMAC-SHA256 do corpo
 * bruto da requisição, usando o App Secret como chave.
 */
@Component
public class AssinaturaValidator {

    private static final String PREFIXO = "sha256=";
    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec chave;

    public AssinaturaValidator(WhatsAppProperties properties) {
        this.chave = new SecretKeySpec(properties.appSecret().getBytes(StandardCharsets.UTF_8), ALGORITMO);
    }

    public boolean valida(byte[] corpo, String assinatura) {
        if (assinatura == null || !assinatura.startsWith(PREFIXO)) {
            return false;
        }
        byte[] recebida;
        try {
            recebida = HexFormat.of().parseHex(assinatura.substring(PREFIXO.length()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(calcular(corpo), recebida);
    }

    private byte[] calcular(byte[] corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            return mac.doFinal(corpo);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 indisponível", ex);
        }
    }
}
