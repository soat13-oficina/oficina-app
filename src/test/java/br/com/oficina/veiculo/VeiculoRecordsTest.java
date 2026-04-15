package br.com.oficina.veiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.infrastructure.web.request.AlterarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.request.CadastrarVeiculoRequest;

class VeiculoRecordsTest {

    @Test
    void deveExporDadosDosCommandsQueriesERequests() {
        CadastrarVeiculoCommand cadastrarCommand = new CadastrarVeiculoCommand(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX,
                "cliente-1");
        AlterarVeiculoCommand alterarCommand = new AlterarVeiculoCommand(
                "ABC1D23",
                "Toyota",
                "Yaris",
                "Toyota Motor Corporation",
                2025,
                120,
                "CVT",
                TipoCombustivel.FLEX);
        ExcluirVeiculoCommand excluirCommand = new ExcluirVeiculoCommand("ABC1D23");
        ConsultarVeiculosQuery consultarQuery = new ConsultarVeiculosQuery(
                "ABC1D23",
                2024,
                "Toyota",
                "Toyota Motor Corporation",
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
        CadastrarVeiculoRequest cadastrarRequest = new CadastrarVeiculoRequest(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX,
                "cliente-1");
        AlterarVeiculoRequest alterarRequest = new AlterarVeiculoRequest(
                "Toyota",
                "Yaris",
                "Toyota Motor Corporation",
                2025,
                120,
                "CVT",
                TipoCombustivel.FLEX);

        assertEquals("ABC1D23", cadastrarCommand.placa());
        assertEquals("cliente-1", cadastrarCommand.clienteId());
        assertEquals("Yaris", alterarCommand.modelo());
        assertEquals("ABC1D23", excluirCommand.placa());
        assertEquals("Toyota Motor Corporation", consultarQuery.fabricante());
        assertEquals("Corolla", cadastrarRequest.modelo());
        assertEquals("CVT", alterarRequest.cambio());
    }
}
