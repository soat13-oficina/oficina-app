package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class OrdemDeServicoTest {

    @Test
    void deveAbrirAlterarEExporDadosDaOrdemDeServico() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-001");
        Cliente novoCliente = Cliente.reconstituir(UUID.fromString("51111111-1111-1111-1111-111111111111"), "Bianca", "99999999999", TipoCliente.PF);
        Veiculo novoVeiculo = Veiculo.reconstituir(
                UUID.fromString("71111111-1111-1111-1111-111111111111"),
                novoCliente.getId(),
                "XYZ9Z99",
                "Honda",
                "City",
                "Honda Motor Co.",
                2023,
                126,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        ordemDeServico.alterar(novoCliente, novoVeiculo);

        assertEquals(UUID.nameUUIDFromBytes("ordem-OS-001".getBytes()), ordemDeServico.getId());
        assertEquals("OS-001", ordemDeServico.getNumeroOrdemServico());
        assertEquals(UUID.nameUUIDFromBytes("funcionario-OS-001".getBytes()), ordemDeServico.getFuncionario().getId());
        assertEquals("Bianca", ordemDeServico.getCliente().getNome());
        assertEquals("XYZ9Z99", ordemDeServico.getVeiculo().getPlaca());
        assertEquals(StatusOrdemDeServico.OS_ABERTA, ordemDeServico.getStatus());
        assertEquals("Joao", ordemDeServico.getFuncionario().getNome());
        assertEquals(null, ordemDeServico.getFuncionario().getCpf());
        assertEquals(UUID.fromString("71111111-1111-1111-1111-111111111111"), ordemDeServico.getVeiculoId());
    }

    @Test
    void deveExecutarFluxoCompletoDaOrdemDeServico() {
        OrdemDeServico ordemDeServico = novaOrdem("OS-002");

        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServico.finalizar();

        assertEquals(StatusOrdemDeServico.OS_FINALIZADA, ordemDeServico.getStatus());
        assertNotNull(ordemDeServico.getIniciadaEm());
        assertNotNull(ordemDeServico.getFinalizadaEm());
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
                        Cliente.reconstituir(UUID.fromString("53333333-3333-3333-3333-333333333333"), "Marcos", "22222222222", TipoCliente.PF),
                        Veiculo.reconstituir(
                                UUID.fromString("73333333-3333-3333-3333-333333333333"),
                                UUID.fromString("53333333-3333-3333-3333-333333333333"),
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
        UUID clienteId = UUID.fromString("51111111-1111-1111-1111-111111111112");
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numeroOrdemServico).getBytes()),
                numeroOrdemServico,
                Funcionario.reconstituir(UUID.nameUUIDFromBytes(("funcionario-" + numeroOrdemServico).getBytes()), "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numeroOrdemServico).getBytes()),
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
