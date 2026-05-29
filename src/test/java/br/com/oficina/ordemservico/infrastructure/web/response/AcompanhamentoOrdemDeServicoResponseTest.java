package br.com.oficina.ordemservico.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class AcompanhamentoOrdemDeServicoResponseTest {

    @Test
    void deveConverterOrdemDeServicoParaResponseDeAcompanhamento() {
        UUID id = UUID.fromString("41111111-1111-1111-1111-111111111112");
        UUID clienteId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        UUID veiculoId = UUID.fromString("42222222-2222-2222-2222-222222222222");
        UUID funcionarioId = UUID.fromString("43333333-3333-3333-3333-333333333333");
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                id,
                "OS-001",
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "11144477735", TipoCliente.PF),
                Veiculo.reconstituir(
                        veiculoId,
                        clienteId,
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));

        AcompanhamentoOrdemDeServicoResponse response = AcompanhamentoOrdemDeServicoResponse.from(ordemDeServico);

        assertEquals("OS-001", response.numeroOrdemServico());
        assertEquals("Maria", response.nomeCliente());
        assertEquals("ABC1D23", response.placaVeiculo());
        assertEquals(StatusOrdemDeServico.OS_ABERTA, response.status());
        assertNull(response.iniciadaEm());
        assertNull(response.finalizadaEm());
        assertNull(response.entregueEm());
    }
}
