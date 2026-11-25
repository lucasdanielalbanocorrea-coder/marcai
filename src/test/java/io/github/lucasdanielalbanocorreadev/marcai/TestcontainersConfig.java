package io.github.lucasdanielalbanocorreadev.marcai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /**
     * Um único Postgres para toda a suíte, mesmo com vários contextos Spring (cada um com
     * propriedades diferentes). Os testes limpam as tabelas no @BeforeEach. Mesma imagem do docker-compose.yml.
     */
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.6-alpine"));

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }

}
