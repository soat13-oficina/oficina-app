package br.com.oficina.common.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.ExcluirPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;

/**
 * Sobe a aplicacao sobre o esquema criado pelas migrations (Flyway) com {@code ddl-auto=validate}:
 * em producao o Hibernate nao cria nada ({@code ddl-auto: none}), entao entidade e migration
 * desalinhadas so apareceriam em runtime. Tambem exercita as reservas da Saga com as restricoes reais
 * do Postgres. (Orcamentos e pagamentos sairam para o Billing Service, com banco proprio; as tabelas
 * antigas ficam aqui ate a migration que as remove, depois do Billing no ar.)
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

    @Autowired
    private ReservaPecaRepository reservas;

    @Autowired
    private ExcluirPecaInsumoUseCase excluirPeca;

    @BeforeEach
    void dados() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // CASCADE leva veiculos, ordens, orcamentos e reservas junto.
        jdbc.execute("TRUNCATE clientes, funcionarios, pecas_insumos CASCADE");
        new ResourceDatabasePopulator(new ClassPathResource("sql/ordens-com-orcamento-aprovado.sql")).execute(dataSource);
    }

    @Test
    void reservaEhUmaLinhaPorOrdemEPecaEAtualizaNoLugar() {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        UUID ordem = UUID.randomUUID();
        reservas.salvar(ReservaPeca.nova(ordem, "oleo", 2, agora));
        reservas.salvar(ReservaPeca.nova(ordem, "filtro", 1, agora));

        reservas.salvar(ReservaPeca.nova(ordem, "filtro", 1, agora).com(ReservaPeca.Status.CONSUMIDA, agora.plusHours(1)));

        assertThat(reservas.buscarPorOrdemDeServico(ordem))
                .extracting(ReservaPeca::pecaInsumoId, ReservaPeca::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("filtro", ReservaPeca.Status.CONSUMIDA),
                        org.assertj.core.groups.Tuple.tuple("oleo", ReservaPeca.Status.RESERVADA));
        assertThatThrownBy(() -> reservas.salvar(ReservaPeca.nova(ordem, "nao-existe", 1, agora)))
                .as("so peca do cadastro").isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pecaComHistoricoDeReservaNaoPodeSerExcluida() {
        reservas.salvar(ReservaPeca.nova(UUID.randomUUID(), "oleo", 1, LocalDateTime.now())
                .com(ReservaPeca.Status.LIBERADA, LocalDateTime.now()));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE pecas_insumos SET quantidade_reservada = 0 WHERE id = 'oleo'");

        assertThatThrownBy(() -> excluirPeca.excluirPecaInsumo(new ExcluirPecaInsumoCommand("oleo")))
                .as("FK de reservas_pecas; a API responde 409").isInstanceOf(DataIntegrityViolationException.class);
    }
}
