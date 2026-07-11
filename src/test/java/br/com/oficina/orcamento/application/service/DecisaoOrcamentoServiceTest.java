package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.DecidirOrcamentoExternamenteCommand;
import br.com.oficina.orcamento.application.result.DecisaoOrcamentoResult;
import br.com.oficina.orcamento.domain.model.DecisaoOrcamento;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class DecisaoOrcamentoServiceTest {

    private static final String NUMERO_OS = "OS-DECISAO-001";
    private static final String NUMERO_ORC = "ORC-DECISAO-001";

    private TestOrcamentoRepository orcamentoRepository;
    private TestOrdemDeServicoRepository ordemDeServicoRepository;
    private DecidirOrcamentoExternamenteService service;

    @BeforeEach
    void setUp() {
        orcamentoRepository = new TestOrcamentoRepository();
        ordemDeServicoRepository = new TestOrdemDeServicoRepository();

        AprovarOrcamentoService aprovarService = new AprovarOrcamentoService(orcamentoRepository, cmd -> {});
        RejeitarOrcamentoService rejeitarService = new RejeitarOrcamentoService(orcamentoRepository);

        service = new DecidirOrcamentoExternamenteService(
                orcamentoRepository,
                ordemDeServicoRepository,
                aprovarService,
                rejeitarService,
                event -> {});
    }

    @Test
    void deveAprovarOrcamentoEMoverOsParaExecucao() {
        OrdemDeServico ordem = ordemEmAguardandoAprovacao();
        Orcamento orc = orcamentoAguardandoAprovacao(ordem.getId());

        DecisaoOrcamentoResult resultado = service.decidir(
                new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.APROVADO));

        assertEquals(NUMERO_ORC, resultado.numeroOrcamento());
        assertEquals(NUMERO_OS, resultado.numeroOrdemServico());
        assertEquals(SituacaoOrdemDeServico.EXECUCAO.getDescricao(), resultado.situacao());

        Orcamento orcAtualizado = orcamentoRepository.buscarPorNumeroOrcamento(NUMERO_ORC).orElseThrow();
        assertEquals(StatusOrcamento.APROVADO, orcAtualizado.getStatus());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero(NUMERO_OS).orElseThrow();
        assertEquals(StatusOrdemDeServico.SERVICO_EM_ANDAMENTO, ordemAtualizada.getStatus());
    }

    @Test
    void deveRejeitarOrcamentoEMoverOsParaFinalizada() {
        OrdemDeServico ordem = ordemEmAguardandoAprovacao();
        Orcamento orc = orcamentoAguardandoAprovacao(ordem.getId());

        DecisaoOrcamentoResult resultado = service.decidir(
                new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.REJEITADO));

        assertEquals(SituacaoOrdemDeServico.FINALIZADA.getDescricao(), resultado.situacao());

        Orcamento orcAtualizado = orcamentoRepository.buscarPorNumeroOrcamento(NUMERO_ORC).orElseThrow();
        assertEquals(StatusOrcamento.REJEITADO, orcAtualizado.getStatus());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero(NUMERO_OS).orElseThrow();
        assertEquals(StatusOrdemDeServico.OS_FINALIZADA, ordemAtualizada.getStatus());
    }

    @Test
    void deveRetornarSituacaoAtualEmRetryIdenticoDeAprovacao() {
        OrdemDeServico ordem = ordemEmAguardandoAprovacao();
        orcamentoAguardandoAprovacao(ordem.getId());

        service.decidir(new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.APROVADO));
        DecisaoOrcamentoResult resultado = service.decidir(
                new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.APROVADO));

        assertNotNull(resultado);
        assertEquals(NUMERO_ORC, resultado.numeroOrcamento());
    }

    @Test
    void deveRejeitarDecisaoDivergente() {
        OrdemDeServico ordem = ordemEmAguardandoAprovacao();
        orcamentoAguardandoAprovacao(ordem.getId());

        service.decidir(new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.APROVADO));

        ConflitoDeRecursoException exception = assertThrows(
                ConflitoDeRecursoException.class,
                () -> service.decidir(new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.REJEITADO)));

        assertEquals("Decisao divergente da decisao ja registrada para o orcamento.", exception.getMessage());
    }

    @Test
    void deveRejeitarDecisaoQuandoOsNaoEstaEmAguardandoAprovacao() {
        UUID ordemId = UUID.randomUUID();
        ordemDeServicoRepository.salvar(OrdemDeServico.reconstituir(
                ordemId, NUMERO_OS, funcionario(), cliente(), veiculo(),
                StatusOrdemDeServico.SERVICO_EM_ANDAMENTO, LocalDateTime.now(), null, null));
        orcamentoAguardandoAprovacao(ordemId);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.decidir(new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, DecisaoOrcamento.APROVADO)));

        assertEquals("Ordem de servico so pode receber decisao quando estiver aguardando aprovacao.", exception.getMessage());
    }

    @Test
    void deveRejeitarDecisaoSemTipoDefinido() {
        ordemEmAguardandoAprovacao();
        orcamentoAguardandoAprovacao(ordemDeServicoRepository.buscarPorNumero(NUMERO_OS).orElseThrow().getId());

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.decidir(new DecidirOrcamentoExternamenteCommand(NUMERO_ORC, null)));

        assertEquals("Decisao do orcamento e obrigatoria.", exception.getMessage());
    }

    private OrdemDeServico ordemEmAguardandoAprovacao() {
        UUID id = UUID.randomUUID();
        OrdemDeServico ordem = OrdemDeServico.reconstituir(
                id, NUMERO_OS, funcionario(), cliente(), veiculo(),
                StatusOrdemDeServico.AGUARDANDO_APROVACAO, LocalDateTime.now(), null, null);
        ordemDeServicoRepository.salvar(ordem);
        return ordemDeServicoRepository.buscarPorNumero(NUMERO_OS).orElseThrow();
    }

    private Orcamento orcamentoAguardandoAprovacao(UUID ordemId) {
        Orcamento orc = new Orcamento(
                NUMERO_ORC,
                UUID.randomUUID(),
                ordemId,
                UUID.randomUUID(),
                "Maria",
                "12345678909",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de oleo",
                List.of("Troca de oleo"),
                List.of(),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                StatusOrcamento.AGUARDANDO_APROVACAO);
        return orcamentoRepository.salvar(orc);
    }

    private static Funcionario funcionario() {
        return Funcionario.reconstituir(UUID.randomUUID(), "Joao", "12345678909");
    }

    private static Cliente cliente() {
        return Cliente.reconstituir(UUID.randomUUID(), "Maria", "12345678909", TipoCliente.PF);
    }

    private static Veiculo veiculo() {
        UUID clienteId = UUID.randomUUID();
        return Veiculo.reconstituir(UUID.randomUUID(), clienteId, "ABC1D23", "Toyota", "Corolla",
                "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX);
    }
}
