package br.com.oficina.common.infrastructure.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Roda as migrations Flyway REAIS (V1 em diante) num PostgreSQL vazio - os demais testes de
 * integracao usam ddl-auto e nunca exercitam o SQL das migrations - e confere o que a Fase 4
 * acrescentou: historico de status (V21), outbox e idempotencia (V22), o advisory lock que
 * impede duas replicas de publicarem o outbox ao mesmo tempo e a regularizacao das reservas (V24).
 *
 * <p>Antes da V24 entram OSs com orcamento ({@code sql/ordens-com-orcamento-aprovado.sql}), como as
 * que existem em producao, para conferir o backfill de {@code reservas_pecas}.
 */
class MigracoesEMensageriaPostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String OS_AGUARDANDO_EXECUCAO = "40000001-0000-0000-0000-000000000001";
    private static final String OS_EM_EXECUCAO = "40000002-0000-0000-0000-000000000001";
    private static DriverManagerDataSource dataSource;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("23").load().migrate();
        new ResourceDatabasePopulator(new ClassPathResource("sql/ordens-com-orcamento-aprovado.sql")).execute(dataSource);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @Test
    void migrationsDaFase4CriamAsTabelasEsperadas() {
        List<String> tabelas = new JdbcTemplate(dataSource).queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);

        assertThat(tabelas).contains("ordem_servico_historico", "outbox_mensagens", "mensagens_processadas",
                "saga_instancia", "saga_passos", "reservas_pecas", "pagamentos");
    }

    @Test
    void v24RegularizaAsReservasDasOrdensJaAprovadasSomandoAsPecasERepetirNaoDuplica() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String consulta = "select ordem_de_servico_id || ':' || peca_insumo_id || ':' || quantidade || ':' || status "
                + "from reservas_pecas order by ordem_de_servico_id, peca_insumo_id";

        // Aguardando aprovacao e finalizada ficam de fora; a peca que saiu do cadastro tambem.
        assertThat(jdbc.queryForList(consulta, String.class)).containsExactly(
                OS_AGUARDANDO_EXECUCAO + ":filtro:3:RESERVADA",
                OS_AGUARDANDO_EXECUCAO + ":oleo:4:RESERVADA",
                OS_EM_EXECUCAO + ":filtro:1:RESERVADA");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V24__criar_reservas_e_pagamentos_da_saga.sql"))
                .execute(dataSource);
        assertThat(jdbc.queryForList(consulta, String.class)).as("idempotente").hasSize(3);
    }

    @Test
    void pagamentoExigeOrcamentoExistenteEOExpiradorTemIndiceParcial() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(jdbc.queryForObject(
                "select indexdef from pg_indexes where indexname = 'idx_pagamentos_pendentes'", String.class))
                .contains("WHERE ((status)::text = 'PENDENTE'::text)");
        assertThat(jdbc.queryForList("""
                select ccu.table_name
                  from information_schema.table_constraints tc
                  join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name
                 where tc.table_name = 'pagamentos' and tc.constraint_type = 'FOREIGN KEY'
                """, String.class)).containsExactly("orcamentos");
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.columns
                 where table_name = 'saga_instancia' and column_name = 'link_pagamento'
                """, Integer.class)).isEqualTo(1);
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
