package br.com.oficina.ordemservico.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
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

        OrdemDeServicoResponse response = OrdemDeServicoResponse.from(ordemDeServico);

        assertEquals("id-1", response.id());
        assertEquals("OS-001", response.numeroOrdemServico());
        assertEquals("cliente-1", response.clienteId());
        assertEquals("Maria", response.nomeCliente());
        assertEquals("111", response.cpfCliente());
        assertEquals("ABC1D23", response.placaVeiculo());
        assertEquals(StatusOrdemDeServico.ABERTA, response.status());
    }
}
