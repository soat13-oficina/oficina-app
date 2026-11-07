package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.EntregarAoClienteCommand;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.ServicoOrdem;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

/**
 * O que ainda e acao direta do OS Service no fim do ciclo da ordem: entrega ao cliente e exclusao.
 * Diagnostico, orcamento, pagamento e execucao chegam a ordem pela Saga, por eventos da Execution e do
 * Billing (coberto no {@code OrquestradorSagaOrdemServicoTest} e no BDD); aqui essas reacoes sao
 * simuladas chamando o agregado.
 */
class FluxoOrdemDeServicoServicesTest {

    @Test
    void deveEntregarAoClienteEExcluirUmaOrdemFinalizada() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        OrdemDeServico ordem = novaOrdem("OS-001");
        ordem.iniciarDiagnostico();
        ordem.registrarDiagnostico(List.of(new ServicoOrdem("Troca de oleo", new java.math.BigDecimal("120.00"))),
                List.of(new PecaPrevistaOrdem("peca-oleo-001", 1)));
        ordem.enviarParaAprovacao();
        ordem.aguardarPagamento();
        ordem.confirmarPagamento();
        ordem.iniciarExecucao();
        ordem.concluirServico();
        repository.salvar(ordem);

        new EntregarAoClienteService(repository).entregarAoCliente(new EntregarAoClienteCommand("OS-001"));

        OrdemDeServico ordemAtualizada = repository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals(StatusOrdemDeServico.ENTREGUE, ordemAtualizada.getStatus());
        assertNotNull(ordemAtualizada.getIniciadaEm());
        assertNotNull(ordemAtualizada.getFinalizadaEm());
        assertNotNull(ordemAtualizada.getEntregueEm());

        new ExcluirOrdemDeServicoService(repository).excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-001"));

        assertTrue(repository.buscarPorNumero("OS-001").isEmpty());
    }

    @Test
    void deveFalharAoExcluirOrdemDeServicoInexistente() {
        ExcluirOrdemDeServicoService service = new ExcluirOrdemDeServicoService(new TestOrdemDeServicoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-404")));

        assertEquals("Ordem de servico nao encontrada para o numero informado.", exception.getMessage());
    }

    private OrdemDeServico novaOrdem(String numero) {
        UUID clienteId = UUID.fromString("61111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("71111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes()),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes()),
                        clienteId,
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
    }
}
