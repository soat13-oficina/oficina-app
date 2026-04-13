package br.com.oficina.ordemservico.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class OrdemDeServicoResponseTest {

    @Test
    void deveConverterOrdemDeServicoParaResponse() {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-1",
                "OS-001",
                new Funcionario("func-1", "Joao", null),
                Cliente.reconstituir(UUID.fromString("41111111-1111-1111-1111-111111111111"), "Maria", "11111111111", TipoCliente.PF),
                new Veiculo(
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));

        OrdemDeServicoResponse response = OrdemDeServicoResponse.from(ordemDeServico);

        assertEquals("id-1", response.id());
        assertEquals("OS-001", response.numeroOrdemServico());
        assertEquals("41111111-1111-1111-1111-111111111111", response.clienteId());
        assertEquals("Maria", response.nomeCliente());
        assertEquals("11111111111", response.documentoCliente());
        assertEquals(TipoCliente.PF, response.tipoCliente());
        assertEquals("ABC1D23", response.placaVeiculo());
        assertEquals(StatusOrdemDeServico.OS_ABERTA, response.status());
    }
}
