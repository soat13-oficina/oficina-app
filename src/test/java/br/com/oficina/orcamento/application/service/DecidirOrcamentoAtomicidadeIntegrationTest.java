package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.DecidirOrcamentoExternamenteCommand;
import br.com.oficina.orcamento.application.usecase.DecidirOrcamentoExternamenteUseCase;
import br.com.oficina.orcamento.domain.model.DecisaoOrcamento;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.JpaOrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/**
 * Valida a atomicidade da decisão externa (CRI-002, spec 006): com `@Transactional` em `decidir()`,
 * a aprovação do orçamento, a reserva de peças e a atualização da OS confirmam juntas ou revertem juntas.
 */
@SpringBootTest
@Import(DecidirOrcamentoAtomicidadeIntegrationTest.FalhaAoSalvarOSConfig.class)
class DecidirOrcamentoAtomicidadeIntegrationTest {

    @Autowired
    private DecidirOrcamentoExternamenteUseCase decidirOrcamentoExternamenteUseCase;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private PecaInsumoRepository pecaInsumoRepository;

    @Autowired
    private SpringDataOrdemDeServicoRepository springDataOrdemDeServicoRepository;

    @Autowired
    private SpringDataOrcamentoRepository springDataOrcamentoRepository;

    @Autowired
    private SpringDataPecaInsumoRepository springDataPecaInsumoRepository;

    @Autowired
    private SpringDataVeiculoRepository springDataVeiculoRepository;

    @Autowired
    private SpringDataClienteRepository springDataClienteRepository;

    @Autowired
    private SpringDataFuncionarioRepository springDataFuncionarioRepository;

    @BeforeEach
    void setUp() {
        limparTabelas();
    }

    // Sem @Transactional (proposital: o teste precisa que o setup esteja
    // realmente commitado para exercitar o rollback do use case como uma
    // transacao separada). Por isso a limpeza tem que rodar tambem depois,
    // senao a ultima OS/cliente ("Maria"/20110101103) fica commitada e
    // polui outras classes de integracao que dividem o mesmo Postgres.
    @AfterEach
    void tearDown() {
        limparTabelas();
    }

    private void limparTabelas() {
        springDataOrcamentoRepository.deleteAll();
        springDataOrdemDeServicoRepository.deleteAll();
        springDataPecaInsumoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();
    }

    @Test
    void deveReverterAprovacaoEReservaQuandoSalvarOSFalha() {
        String pecaId = seedPeca("PINS-ATOM-1", 10, 0);
        UUID ordemId = seedOrdemEmAguardandoAprovacao();
        seedOrcamento("ORC-ATOM-1", ordemId, pecaId, 2);

        assertThrows(RuntimeException.class, () -> decidirOrcamentoExternamenteUseCase.decidir(
                new DecidirOrcamentoExternamenteCommand("ORC-ATOM-1", DecisaoOrcamento.APROVADO)));

        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento("ORC-ATOM-1").orElseThrow();
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, orcamento.getStatus(),
                "Orçamento deve permanecer aguardando aprovação após o rollback");
        assertEquals(0, pecaInsumoRepository.buscarPorId(pecaId).orElseThrow().getQuantidadeReservada(),
                "Nenhuma peça pode permanecer reservada após o rollback");
    }

    @Test
    void devePreservarOrcamentoQuandoEstoqueInsuficiente() {
        String pecaId = seedPeca("PINS-ATOM-2", 1, 0);
        UUID ordemId = seedOrdemEmAguardandoAprovacao();
        seedOrcamento("ORC-ATOM-2", ordemId, pecaId, 5);

        assertThrows(RegraDeNegocioException.class, () -> decidirOrcamentoExternamenteUseCase.decidir(
                new DecidirOrcamentoExternamenteCommand("ORC-ATOM-2", DecisaoOrcamento.APROVADO)));

        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento("ORC-ATOM-2").orElseThrow();
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, orcamento.getStatus(),
                "Orçamento deve permanecer aguardando aprovação quando o estoque é insuficiente");
        assertEquals(0, pecaInsumoRepository.buscarPorId(pecaId).orElseThrow().getQuantidadeReservada(),
                "Nenhuma peça pode permanecer reservada quando o estoque é insuficiente");
    }

    private String seedPeca(String codigoReferencia, int estoque, int reservada) {
        PecaInsumo peca = new PecaInsumo(
                UUID.randomUUID().toString(), "Pastilha de freio", "Bosch", new BigDecimal("80.00"),
                estoque, reservada, codigoReferencia, CategoriaPeca.FREIOS);
        pecaInsumoRepository.salvar(peca);
        return peca.getId();
    }

    private UUID seedOrdemEmAguardandoAprovacao() {
        Cliente cliente = springDataClienteRepository.save(new Cliente("Maria", "20110101103", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        Veiculo veiculo = springDataVeiculoRepository.save(new Veiculo(
                cliente.getId(), "ATM1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        OrdemDeServico ordem = OrdemDeServico.reconstituir(
                null,
                "OS-ATOM-" + UUID.randomUUID().toString().substring(0, 8),
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                Cliente.reconstituir(cliente.getId(), cliente.getNome(), cliente.getCpfOuCnpj(), TipoCliente.PF),
                veiculo,
                StatusOrdemDeServico.AGUARDANDO_APROVACAO,
                LocalDateTime.now(), null, null);
        return springDataOrdemDeServicoRepository.save(ordem).getId();
    }

    private void seedOrcamento(String numero, UUID ordemId, String pecaId, int quantidade) {
        OrdemDeServico ordem = springDataOrdemDeServicoRepository.findById(ordemId).orElseThrow();
        PecaOrcamento pecaOrcamento = new PecaOrcamento(pecaId, "Pastilha de freio", new BigDecimal("80.00"), quantidade);
        springDataOrcamentoRepository.save(new Orcamento(
                numero,
                ordem.getCliente().getId(),
                ordem.getId(),
                ordem.getFuncionario().getId(),
                ordem.getCliente().getNome(),
                ordem.getCliente().getCpfOuCnpj(),
                ordem.getVeiculo().getPlaca(),
                ordem.getVeiculo().getMarca(),
                ordem.getVeiculo().getModelo(),
                "Descricao diagnostico",
                List.of("Servico 1"),
                List.of(pecaOrcamento),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                StatusOrcamento.AGUARDANDO_APROVACAO));
    }

    /**
     * Substitui o adaptador da OS por um que falha ao salvar, para exercitar o rollback transacional
     * da decisão. Não usa Mockito (restrição da constituição) — é um duplo de teste escrito à mão.
     */
    @TestConfiguration
    static class FalhaAoSalvarOSConfig {
        @Bean
        @Primary
        OrdemDeServicoRepository ordemDeServicoRepositoryQueFalhaAoSalvar(SpringDataOrdemDeServicoRepository springData) {
            return new JpaOrdemDeServicoRepository(springData) {
                @Override
                public void salvar(OrdemDeServico ordemDeServico) {
                    throw new IllegalStateException("Falha simulada ao salvar a ordem de servico");
                }
            };
        }
    }
}
