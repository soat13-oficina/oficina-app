package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * V17: OS em AGUARDANDO_APROVACAO sem orcamento voltam para DIAGNOSTICO_CONCLUIDO. Roda no esquema
 * do Flyway (Postgres): a tabela {@code orcamentos} nao tem mais entidade neste servico (o orcamento
 * mora no Billing desde o Passo 3), mas continua no banco ate a migration que a remove.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///oficina_esquema",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class RegularizacaoOrdensAguardandoAprovacaoIntegrationTest {

    private static final String COM_ORCAMENTO = "40000003-0000-0000-0000-000000000001";
    private static final String ORFA = "40000009-0000-0000-0000-000000000001";

    @Autowired
    private DataSource dataSource;

    @Test
    void deveReverterOrdensSemOrcamentoEPreservarAsComOrcamento() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("TRUNCATE clientes, funcionarios, pecas_insumos CASCADE");
        // OS-V24-3 esta em AGUARDANDO_APROVACAO com orcamento.
        new ResourceDatabasePopulator(new ClassPathResource("sql/ordens-com-orcamento-aprovado.sql")).execute(dataSource);
        // OS orfa: o estado legado, AGUARDANDO_APROVACAO sem orcamento.
        jdbc.update("""
                INSERT INTO ordens_de_servico (id, numero_ordem_servico, funcionario_id, funcionario_nome, cliente_id, veiculo_id,
                  cliente_nome, veiculo_placa, veiculo_marca, veiculo_modelo, veiculo_fabricante, veiculo_ano, veiculo_potencia,
                  veiculo_cambio, veiculo_tipo, status)
                SELECT ?::uuid, 'OS-ORFA', funcionario_id, funcionario_nome, cliente_id, veiculo_id, cliente_nome, veiculo_placa,
                  veiculo_marca, veiculo_modelo, veiculo_fabricante, veiculo_ano, veiculo_potencia, veiculo_cambio, veiculo_tipo,
                  'AGUARDANDO_APROVACAO'
                FROM ordens_de_servico WHERE id = ?::uuid
                """, ORFA, COM_ORCAMENTO);

        String sql = carregarMigracaoV17();

        assertEquals(1, jdbc.update(sql), "apenas a OS orfa deve ser regularizada");
        assertEquals("DIAGNOSTICO_CONCLUIDO", status(jdbc, ORFA), "OS orfa revertida");
        assertEquals("AGUARDANDO_APROVACAO", status(jdbc, COM_ORCAMENTO), "OS com orcamento preservada");
        assertEquals(0, jdbc.update(sql), "migracao e idempotente");
    }

    private static String status(JdbcTemplate jdbc, String id) {
        return jdbc.queryForObject("SELECT status FROM ordens_de_servico WHERE id = ?::uuid", String.class, id);
    }

    private static String carregarMigracaoV17() throws Exception {
        byte[] bytes = new ClassPathResource(
                "db/migration/V17__regularizar_ordens_aguardando_aprovacao_sem_orcamento.sql")
                .getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8).lines()
                .filter(linha -> !linha.trim().startsWith("--"))
                .reduce("", (acc, linha) -> acc + linha + "\n")
                .trim();
    }
}
