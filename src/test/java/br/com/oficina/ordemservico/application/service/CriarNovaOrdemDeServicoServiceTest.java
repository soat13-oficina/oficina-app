package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.infrastructure.persistence.InMemoryClienteRepository;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.infrastructure.persistence.InMemoryOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.InMemoryVeiculoRepository;

class CriarNovaOrdemDeServicoServiceTest {

    @Test
    void deveCriarNovaOrdemDeServico() {
        InMemoryClienteRepository clienteRepository = new InMemoryClienteRepository();
        InMemoryVeiculoRepository veiculoRepository = new InMemoryVeiculoRepository();
        InMemoryOrdemDeServicoRepository ordemDeServicoRepository = new InMemoryOrdemDeServicoRepository();
        clienteRepository.salvar(new Cliente("cliente-1", "Maria", "111"));
        veiculoRepository.salvar(new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                ordemDeServicoRepository);

        service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand("cliente-1", "func-1", "ABC1D23"));

        assertEquals(1, ordemDeServicoRepository.buscarTodas().size());
        assertEquals("cliente-1", ordemDeServicoRepository.buscarTodas().get(0).getCliente().getId());
    }

    @Test
    void deveFalharQuandoClienteNaoExistir() {
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                new InMemoryClienteRepository(),
                new InMemoryVeiculoRepository(),
                new InMemoryOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand("cliente-1", "func-1", "ABC1D23")));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }
}
