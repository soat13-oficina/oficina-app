package br.com.oficina.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integração de persistência. Sobe um PostgreSQL real via Testcontainers,
 * garantindo que os testes rodem em qualquer máquina com Docker, sem depender de um banco externo
 * apontado em localhost.
 *
 * <p>Usa o padrão <em>singleton container</em>: o container é iniciado uma única vez no bloco
 * estático e permanece de pé durante toda a JVM (sendo removido pelo Ryuk do Testcontainers ao
 * final). Propositalmente NÃO usamos {@code @Testcontainers}/{@code @Container}, pois essas
 * anotações param o container ao fim de cada classe de teste — e como o contexto Spring é cacheado
 * e reaproveitado entre as classes, o datasource passaria a apontar para um container já encerrado.
 * Mantendo um único container vivo, o contexto cacheado continua válido. O {@code @ServiceConnection}
 * injeta automaticamente url/usuário/senha no datasource.
 */
@SpringBootTest
@ActiveProfiles("integration")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
