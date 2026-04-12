package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class InMemoryOrdemDeServicoRepositoryTest {

    @Test
    void deveSalvarBuscarListarEExcluirOrdemDeServico() {
        InMemoryOrdemDeServicoRepository repository = new InMemoryOrdemDeServicoRepository();
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-1",
                "OS-001",
                new Funcionario("func-1", "Joao", null),
                new Cliente("cliente-1", "Maria", "111"),
                new Veiculo(
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));

        repository.salvar(ordemDeServico);

        assertEquals("OS-001", repository.buscarPorNumero("OS-001").orElseThrow().getNumeroOrdemServico());
        assertEquals(1, repository.buscarTodas().size());

        repository.excluirPorNumero("OS-001");

        assertTrue(repository.buscarPorNumero("OS-001").isEmpty());
        assertEquals(0, repository.buscarTodas().size());
    }
}
