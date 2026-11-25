package io.github.lucasdanielalbanocorreadev.marcai.shared.config;

import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Timeouts vêm de spring.http.clients.* no application.yml. */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    RestClient whatsAppRestClient(RestClient.Builder builder, WhatsAppProperties properties) {
        return builder
                .baseUrl(properties.apiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .build();
    }
}
