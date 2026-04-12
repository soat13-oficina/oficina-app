package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class OrdemDeServicoTest {

    @Test
    void deveAbrirAlterarEExporDadosDaOrdemDeServico() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-001");
        Funcionario novoFuncionario = new Funcionario("func-2", "Paula", "123");
        Cliente novoCliente = new Cliente("cliente-2", "Bianca", "999");
        Veiculo novoVeiculo = new Veiculo(
                "XYZ9Z99",
                "Honda",
                "City",
                "Honda Motor Co.",
                2023,
                126,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        ordemDeServico.alterar(novoFuncionario, novoCliente, novoVeiculo);

        assertEquals("id-OS-001", ordemDeServico.getId());
        assertEquals("OS-001", ordemDeServico.getNumeroOrdemServico());
        assertEquals("func-2", ordemDeServico.getFuncionario().getId());
        assertEquals("Bianca", ordemDeServico.getCliente().getNome());
        assertEquals("XYZ9Z99", ordemDeServico.getVeiculo().getPlaca());
        assertEquals(StatusOrdemDeServico.ABERTA, ordemDeServico.getStatus());
        assertEquals("Paula", ordemDeServico.getFuncionario().getNome());
        assertEquals("123", ordemDeServico.getFuncionario().getCpf());
    }

    @Test
    void deveExecutarFluxoCompletoDaOrdemDeServico() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-002");

        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServico.finalizar();

        assertEquals(StatusOrdemDeServico.FINALIZADA, ordemDeServico.getStatus());
    }

    @Test
    void naoDeveIniciarDiagnosticoQuandoOrdemNaoEstiverAberta() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-003");
        ordemDeServico.iniciarDiagnostico();

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                ordemDeServico::iniciarDiagnostico);

        assertEquals("Diagnostico so pode ser iniciado para ordem aberta", exception.getMessage());
    }

    @Test
    void naoDeveConcluirDiagnosticoQuandoNaoEstiverEmAndamento() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-004");

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                ordemDeServico::concluirDiagnostico);

        assertEquals("Diagnostico so pode ser concluido em andamento", exception.getMessage());
    }

    @Test
    void naoDeveFinalizarSemDiagnosticoConcluido() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-005");

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                ordemDeServico::finalizar);

        assertEquals("Ordem de servico so pode ser finalizada com diagnostico concluido", exception.getMessage());
    }

    @Test
    void naoDeveAlterarQuandoOrdemNaoEstiverAberta() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-006");
        ordemDeServico.iniciarDiagnostico();

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> ordemDeServico.alterar(
                        new Funcionario("func-3", "Carlos", null),
                        new Cliente("cliente-3", "Marcos", "222"),
                        new Veiculo(
                                "TTT1T11",
                                "Jeep",
                                "Renegade",
                                "Stellantis",
                                2024,
                                185,
                                "AUTOMATICO",
                                TipoCombustivel.DIESEL)));

        assertEquals("Ordem de servico so pode ser alterada enquanto estiver aberta", exception.getMessage());
    }

    private OrdemDeServico novaOrdem(String numeroOrdemServico) {
        return OrdemDeServico.abrir(
                "id-" + numeroOrdemServico,
                numeroOrdemServico,
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
    }
}
