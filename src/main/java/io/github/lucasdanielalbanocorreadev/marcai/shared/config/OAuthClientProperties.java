package io.github.lucasdanielalbanocorreadev.marcai.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais do cliente que obtém tokens da API (variáveis OAUTH_CLIENT_ID e OAUTH_CLIENT_SECRET).
 * Sem valor padrão: a aplicação não sobe sem elas.
 */
@Validated
@ConfigurationProperties("oauth.client")
public record OAuthClientProperties(@NotBlank String id, @NotBlank String secret) {
}
