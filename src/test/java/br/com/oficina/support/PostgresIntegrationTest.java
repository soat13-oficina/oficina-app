package br.com.oficina.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração de persistência. Sobe um PostgreSQL real via Testcontainers,
 * garantindo que os testes rodem em qualquer máquina com Docker, sem depender de um banco externo
 * apontado em localhost. O container é estático e compartilhado (padrão singleton) entre as
 * subclasses, e o {@code @ServiceConnection} injeta automaticamente url/usuario/senha no datasource.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
