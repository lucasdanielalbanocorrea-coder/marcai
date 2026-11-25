package io.github.lucasdanielalbanocorreadev.marcai.shared.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A própria aplicação emite tokens (Authorization Server, grant client_credentials) e os valida na API.
 *
 * <pre>
 * curl -u "$OAUTH_CLIENT_ID:$OAUTH_CLIENT_SECRET" -d grant_type=client_credentials http://localhost:8080/oauth2/token
 * curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/clientes
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    RegisteredClientRepository registeredClientRepository(OAuthClientProperties cliente) {
        RegisteredClient painel = RegisteredClient.withId("painel")
                .clientId(cliente.id())
                .clientSecret(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(cliente.secret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(30)).build())
                .build();
        return new InMemoryRegisteredClientRepository(painel);
    }

    /** Endpoints /oauth2/** (token, jwks...). */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
        http.oauth2AuthorizationServer(authorizationServer ->
                http.securityMatcher(authorizationServer.getEndpointsMatcher()));
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        return http.build();
    }

    /** API REST stateless com Bearer JWT; o webhook da Meta é público e validado por assinatura. */
    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/webhook/whatsapp").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sem cookies de sessão não há o que proteger com CSRF.
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
