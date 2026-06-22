package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class ListagemPriorizadaIntegrationTest {

    @Autowired
    private JpaOrdemDeServicoRepository repository;

    @Autowired
    private SpringDataVeiculoRepository veiculoRepository;

    @Test
    void deveOrdenarPorPrioridadeDeSituacao() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Joao", "12345678901");
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF);

        Veiculo v1 = salvarVeiculo(clienteId, "PRI1A11");
        Veiculo v2 = salvarVeiculo(clienteId, "PRI2B22");
        Veiculo v3 = salvarVeiculo(clienteId, "PRI3C33");

        repository.salvar(ordemComStatus("OS-PRIORIDADE-RECV", StatusOrdemDeServico.OS_ABERTA, funcionario, cliente, v1));
        repository.salvar(ordemComStatus("OS-PRIORIDADE-DIAG", StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, funcionario, cliente, v2,
                LocalDateTime.now().minusHours(1)));
        repository.salvar(ordemComStatus("OS-PRIORIDADE-EXEC", StatusOrdemDeServico.SERVICO_EM_ANDAMENTO, funcionario, cliente, v3,
                LocalDateTime.now().minusHours(2)));

        List<OrdemDeServico> resultado = repository.buscarAtivasPriorizadasPorFiltros(null, null, null, null);

        assertEquals(3, resultado.size());
        assertEquals("OS-PRIORIDADE-EXEC", resultado.get(0).getNumeroOrdemServico());
        assertEquals("OS-PRIORIDADE-DIAG", resultado.get(1).getNumeroOrdemServico());
        assertEquals("OS-PRIORIDADE-RECV", resultado.get(2).getNumeroOrdemServico());
    }

    @Test
    void deveExcluirFinalizadasEEntregues() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Carlos", "98765432100");
        Cliente cliente = Cliente.reconstituir(clienteId, "Ana", "98765432100", TipoCliente.PF);

        Veiculo v1 = salvarVeiculo(clienteId, "EXC1A11");
        Veiculo v2 = salvarVeiculo(clienteId, "EXC2B22");
        Veiculo v3 = salvarVeiculo(clienteId, "EXC3C33");

        repository.salvar(ordemComStatus("OS-EXC-ATIVA", StatusOrdemDeServico.OS_ABERTA, funcionario, cliente, v1));
        repository.salvar(ordemComStatus("OS-EXC-FINAL", StatusOrdemDeServico.OS_FINALIZADA, funcionario, cliente, v2,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)));
        repository.salvar(ordemComStatus("OS-EXC-ENTREGA", StatusOrdemDeServico.ENTREGUE, funcionario, cliente, v3,
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2)));

        List<OrdemDeServico> resultado = repository.buscarAtivasPriorizadasPorFiltros(null, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("OS-EXC-ATIVA", resultado.get(0).getNumeroOrdemServico());
        assertFalse(resultado.stream().anyMatch(o -> o.getStatus() == StatusOrdemDeServico.OS_FINALIZADA));
        assertFalse(resultado.stream().anyMatch(o -> o.getStatus() == StatusOrdemDeServico.ENTREGUE));
    }

    @Test
    void deveOrdenarPorAntiguidade_DentroDeUmaMesmaSituacao() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Paulo", "11122233344");
        Cliente cliente = Cliente.reconstituir(clienteId, "Rosa", "11122233344", TipoCliente.PF);

        Veiculo v1 = salvarVeiculo(clienteId, "ANT1A11");
        Veiculo v2 = salvarVeiculo(clienteId, "ANT2B22");

        LocalDateTime maisAntiga = LocalDateTime.now().minusHours(5);
        LocalDateTime maisRecente = LocalDateTime.now().minusHours(1);

        repository.salvar(ordemComStatus("OS-ANT-RECENTE", StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, funcionario, cliente, v1, maisRecente));
        repository.salvar(ordemComStatus("OS-ANT-ANTIGA", StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, funcionario, cliente, v2, maisAntiga));

        List<OrdemDeServico> resultado = repository.buscarAtivasPriorizadasPorFiltros(null, null, null, null);

        assertEquals(2, resultado.size());
        assertEquals("OS-ANT-ANTIGA", resultado.get(0).getNumeroOrdemServico());
        assertEquals("OS-ANT-RECENTE", resultado.get(1).getNumeroOrdemServico());
    }

    private Veiculo salvarVeiculo(UUID clienteId, String placa) {
        return veiculoRepository.save(new Veiculo(
                clienteId, placa, "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
    }

    private OrdemDeServico ordemComStatus(
            String numero,
            StatusOrdemDeServico status,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo) {
        return ordemComStatus(numero, status, funcionario, cliente, veiculo, null, null);
    }

    private OrdemDeServico ordemComStatus(
            String numero,
            StatusOrdemDeServico status,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            LocalDateTime iniciadaEm) {
        return ordemComStatus(numero, status, funcionario, cliente, veiculo, iniciadaEm, null);
    }

    private OrdemDeServico ordemComStatus(
            String numero,
            StatusOrdemDeServico status,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm) {
        return OrdemDeServico.reconstituir(
                null, numero, funcionario, cliente, veiculo, status, iniciadaEm, finalizadaEm, null);
    }
}
