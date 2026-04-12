package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.infrastructure.persistence.InMemoryOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class FluxoOrdemDeServicoServicesTest {

    @Test
    void deveIniciarConcluirFinalizarEExcluirOrdemDeServico() {
        InMemoryOrdemDeServicoRepository repository = new InMemoryOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-001"));

        new IniciarDiagnosticoService(repository).iniciarDiagnostico(new IniciarDiagnosticoCommand("OS-001"));
        new ConcluirDiagnosticoService(repository).concluirDiagnostico(new ConcluirDiagnosticoCommand("OS-001"));
        new FinalizarOrdemDeServicoService(repository).finalizarOrdemDeServico(new FinalizarOrdemDeServicoCommand("OS-001"));

        assertEquals(StatusOrdemDeServico.FINALIZADA, repository.buscarPorNumero("OS-001").orElseThrow().getStatus());

        new ExcluirOrdemDeServicoService(repository).excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-001"));

        assertTrue(repository.buscarPorNumero("OS-001").isEmpty());
    }

    @Test
    void deveFalharAoExcluirOrdemDeServicoInexistente() {
        ExcluirOrdemDeServicoService service = new ExcluirOrdemDeServicoService(new InMemoryOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-404")));

        assertEquals("Ordem de servico nao encontrada", exception.getMessage());
    }

    private OrdemDeServico novaOrdem(String numero) {
        return OrdemDeServico.abrir(
                "id-" + numero,
                numero,
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
