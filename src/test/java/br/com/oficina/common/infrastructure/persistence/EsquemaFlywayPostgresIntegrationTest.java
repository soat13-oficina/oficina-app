package br.com.oficina.common.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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

import br.com.oficina.pagamento.domain.model.Pagamento;
import br.com.oficina.pagamento.domain.model.StatusPagamento;
import br.com.oficina.pagamento.domain.repository.PagamentoRepository;
import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.ExcluirPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;

/**
 * Sobe a aplicacao sobre o esquema criado pelas migrations (Flyway) com {@code ddl-auto=validate}:
 * em producao o Hibernate nao cria nada ({@code ddl-auto: none}), entao entidade e migration
 * desalinhadas so apareceriam em runtime. Tambem exercita os repositorios novos da Saga (reservas e
 * pagamentos) com as restricoes reais do Postgres.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///oficina_esquema",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class EsquemaFlywayPostgresIntegrationTest {

    private static final UUID ORDEM = UUID.fromString("40000001-0000-0000-0000-000000000001");
    private static final UUID ORCAMENTO = UUID.fromString("50000001-0000-0000-0000-000000000001");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PagamentoRepository pagamentos;

    @Autowired
    private ReservaPecaRepository reservas;

    @Autowired
    private ExcluirPecaInsumoUseCase excluirPeca;

    @BeforeEach
    void dados() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // CASCADE leva veiculos, ordens, orcamentos, pagamentos e reservas junto.
        jdbc.execute("TRUNCATE clientes, funcionarios, pecas_insumos CASCADE");
        new ResourceDatabasePopulator(new ClassPathResource("sql/ordens-com-orcamento-aprovado.sql")).execute(dataSource);
    }

    @Test
    void pagamentoGravaEConsultaComoOBillingPrecisa() {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        Pagamento antigo = Pagamento.novo(ORCAMENTO, ORDEM, UUID.randomUUID(), new BigDecimal("100.00"), agora.minusMinutes(1), agora.minusHours(1));
        antigo.recusar("PAY-1", "Cartao recusado", agora.minusMinutes(30));
        pagamentos.salvar(antigo);
        Pagamento atual = Pagamento.novo(ORCAMENTO, ORDEM, UUID.randomUUID(), new BigDecimal("100.00"), agora.minusMinutes(1), agora);
        atual.registrarCobranca("PREF-2", "https://checkout/2", agora);
        pagamentos.salvar(atual);

        assertThat(pagamentos.buscarPorOrcamento(ORCAMENTO)).extracting(Pagamento::getId)
                .as("mais recente primeiro").containsExactly(atual.getId(), antigo.getId());
        assertThat(pagamentos.buscarPendentesVencidos(agora)).extracting(Pagamento::getId).containsExactly(atual.getId());

        Pagamento lido = pagamentos.buscarPorId(atual.getId()).orElseThrow();
        assertThat(lido.getLinkPagamento()).isEqualTo("https://checkout/2");
        assertThat(lido.getValor()).isEqualByComparingTo("100.00");
        lido.aprovar("PAY-2", agora);
        pagamentos.salvar(lido);
        assertThat(pagamentos.buscarPorId(atual.getId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void pagamentoSemOrcamentoEReferenciaDoProvedorRepetidaSaoRecusados() {
        LocalDateTime agora = LocalDateTime.now();
        Pagamento orfao = Pagamento.novo(UUID.randomUUID(), ORDEM, UUID.randomUUID(), BigDecimal.TEN, agora, agora);
        assertThatThrownBy(() -> pagamentos.salvar(orfao)).isInstanceOf(DataIntegrityViolationException.class);

        Pagamento primeiro = Pagamento.novo(ORCAMENTO, ORDEM, UUID.randomUUID(), BigDecimal.TEN, agora, agora);
        primeiro.aprovar("PAY-REPETIDO", agora);
        pagamentos.salvar(primeiro);
        Pagamento segundo = Pagamento.novo(ORCAMENTO, ORDEM, UUID.randomUUID(), BigDecimal.TEN, agora, agora);
        segundo.aprovar("PAY-REPETIDO", agora);
        assertThatThrownBy(() -> pagamentos.salvar(segundo)).isInstanceOf(DataIntegrityViolationException.class);
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
