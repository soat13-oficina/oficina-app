package br.com.oficina.common.infrastructure.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Roda as migrations Flyway REAIS (V1 em diante) num PostgreSQL vazio - os demais testes de
 * integracao usam ddl-auto e nunca exercitam o SQL das migrations - e confere o que a Fase 4
 * acrescentou: historico de status (V21), outbox e idempotencia (V22) e o advisory lock que
 * impede duas replicas de publicarem o outbox ao mesmo tempo.
 */
class MigracoesEMensageriaPostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static DriverManagerDataSource dataSource;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @Test
    void migrationsDaFase4CriamAsTabelasEsperadas() {
        List<String> tabelas = new JdbcTemplate(dataSource).queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);

        assertThat(tabelas).contains("ordem_servico_historico", "outbox_mensagens", "mensagens_processadas");
    }

    @Test
    void historicoEhApagadoJuntoComAOrdem() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> colunasComCascade = jdbc.queryForList("""
                select rc.delete_rule
                  from information_schema.referential_constraints rc
                  join information_schema.table_constraints tc on tc.constraint_name = rc.constraint_name
                 where tc.table_name = 'ordem_servico_historico'
                """, String.class);

        assertThat(colunasComCascade).containsExactly("CASCADE");
    }

    @Test
    void soUmaReplicaPublicaOOutboxPorVez() {
        TransactionTemplate primeira = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TravaPublicacaoOutbox trava = new TravaPublicacaoOutbox(new JdbcTemplate(dataSource), true);
        // Outro DataSource = outra conexao, como outra replica (o mesmo DataSource reaproveitaria a
        // conexao presa a transacao desta thread, e o advisory lock e reentrante na mesma sessao).
        var dataSourceDaOutraReplica = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        TravaPublicacaoOutbox outraReplica = new TravaPublicacaoOutbox(new JdbcTemplate(dataSourceDaOutraReplica), true);

        primeira.executeWithoutResult(status -> {
            assertThat(trava.tentarAdquirir()).isTrue();
            assertThat(outraReplica.tentarAdquirir()).as("enquanto a primeira transacao segura a trava").isFalse();
        });

        Boolean depois = primeira.execute(status -> outraReplica.tentarAdquirir());
        assertThat(depois).as("liberada no fim da transacao").isTrue();
    }

    @Test
    void travaDetectaOPostgresPeloDataSource() {
        TransactionTemplate transacao = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TravaPublicacaoOutbox trava = new TravaPublicacaoOutbox(new JdbcTemplate(dataSource), dataSource);

        Boolean adquirida = transacao.execute(status -> trava.tentarAdquirir());
        assertThat(adquirida).isTrue();
    }
}
