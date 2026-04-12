package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.infrastructure.persistence.InMemoryClienteRepository;
import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.infrastructure.persistence.InMemoryOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.InMemoryVeiculoRepository;

class AlterarOrdemDeServicoServiceTest {

    @Test
    void deveAlterarOrdemDeServicoExistente() {
        InMemoryClienteRepository clienteRepository = new InMemoryClienteRepository();
        InMemoryVeiculoRepository veiculoRepository = new InMemoryVeiculoRepository();
        InMemoryOrdemDeServicoRepository ordemDeServicoRepository = new InMemoryOrdemDeServicoRepository();
        clienteRepository.salvar(new Cliente("cliente-1", "Maria", "111"));
        clienteRepository.salvar(new Cliente("cliente-2", "Bianca", "222"));
        veiculoRepository.salvar(novoVeiculo("ABC1D23", "Toyota"));
        veiculoRepository.salvar(novoVeiculo("XYZ9Z99", "Honda"));
        ordemDeServicoRepository.salvar(OrdemDeServico.abrir(
                "id-1",
                "OS-001",
                new Funcionario("func-1", "Joao", null),
                new Cliente("cliente-1", "Maria", "111"),
                novoVeiculo("ABC1D23", "Toyota")));
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                ordemDeServicoRepository);

        service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-001", "cliente-2", "func-2", "XYZ9Z99"));

        OrdemDeServico alterada = ordemDeServicoRepository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals("cliente-2", alterada.getCliente().getId());
        assertEquals("XYZ9Z99", alterada.getVeiculo().getPlaca());
        assertEquals("func-2", alterada.getFuncionario().getId());
    }

    @Test
    void deveFalharQuandoOrdemNaoExistir() {
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                new InMemoryClienteRepository(),
                new InMemoryVeiculoRepository(),
                new InMemoryOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-404", "cliente-1", "func-1", "ABC1D23")));

        assertEquals("Ordem de servico nao encontrada", exception.getMessage());
    }

    private Veiculo novoVeiculo(String placa, String marca) {
        return new Veiculo(
                placa,
                marca,
                "Modelo",
                "Fabricante",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
    }
}
