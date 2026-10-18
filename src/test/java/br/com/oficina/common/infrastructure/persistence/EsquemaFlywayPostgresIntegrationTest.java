package br.com.oficina.common.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sobe a aplicacao sobre o esquema criado pelas migrations (Flyway) com {@code ddl-auto=validate}:
 * em producao o Hibernate nao cria nada ({@code ddl-auto: none}), entao entidade e migration
 * desalinhadas so apareceriam em runtime. Se o contexto sobe, toda entidade do OS bate com o esquema.
 * (Orcamentos e pagamentos sairam para o Billing Service e estoque, diagnostico e execucao, para a
 * Execution Service, cada um com banco proprio; as tabelas antigas ficam aqui ate a migration que as
 * remove, depois dos dois no ar.)
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///oficina_esquema",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class EsquemaFlywayPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void aplicacaoSobeSobreOEsquemaDasMigrationsEAsTabelasDoOsExistem() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tabelas).contains("ordens_de_servico", "ordem_servico_historico", "saga_instancia", "saga_passos",
                "outbox_mensagens", "mensagens_processadas", "clientes", "veiculos", "funcionarios", "notificacoes");
    }
}
