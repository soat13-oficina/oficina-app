package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.application.command.PecaOrcamentoInput;
import br.com.oficina.orcamento.application.service.CadastrarNovoOrcamentoService;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.ConcluirServicoCommand;
import br.com.oficina.ordemservico.application.command.EntregarAoClienteCommand;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.IniciarExecucaoCommand;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class FluxoOrdemDeServicoServicesTest {

    private static final String PECA_ID = "peca-oleo-001";

    @Test
    void deveIniciarConcluirFinalizarEntregarEExcluirOrdemDeServico() {
        UUID clienteId = UUID.fromString("61111111-1111-1111-1111-111111111111");
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID, "Oleo 5W30", "Castrol", new BigDecimal("150.00"), 10, 0, "REF-OLEO", CategoriaPeca.LUBRIFICANTES));
        repository.salvar(novaOrdem("OS-001"));

        new IniciarDiagnosticoService(repository).iniciarDiagnostico(new IniciarDiagnosticoCommand("OS-001"));
        new ConcluirDiagnosticoService(repository).concluirDiagnostico(new ConcluirDiagnosticoCommand("OS-001"));
        new EnviarDiagnosticoParaOrcamentoService(
                repository,
                new CadastrarNovoOrcamentoService(orcamentoRepository, clienteRepository, pecaInsumoRepository),
                event -> {})
                .enviarDiagnosticoParaOrcamento(new EnviarDiagnosticoParaOrcamentoRequest(
                        "OS-001",
                        "Diagnostico completo do veiculo",
                        List.of("Troca de oleo"),
                        List.of(new PecaOrcamentoInput(PECA_ID, 1)),
                        new BigDecimal("200.00"),
                        BigDecimal.ZERO,
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        null));
        new IniciarExecucaoService(repository, event -> {})
                .iniciarExecucao(new IniciarExecucaoCommand("OS-001"));
        new ConcluirServicoService(repository, event -> {})
                .concluirServico(new ConcluirServicoCommand("OS-001"));
        new EntregarAoClienteService(repository)
                .entregarAoCliente(new EntregarAoClienteCommand("OS-001"));

        OrdemDeServico ordemAtualizada = repository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals(StatusOrdemDeServico.ENTREGUE, ordemAtualizada.getStatus());
        assertNotNull(ordemAtualizada.getIniciadaEm());
        assertNotNull(ordemAtualizada.getFinalizadaEm());
        assertNotNull(ordemAtualizada.getEntregueEm());

        new ExcluirOrdemDeServicoService(repository).excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-001"));

        assertTrue(repository.buscarPorNumero("OS-001").isEmpty());
    }

    @Test
    void deveFalharAoExcluirOrdemDeServicoInexistente() {
        ExcluirOrdemDeServicoService service = new ExcluirOrdemDeServicoService(new TestOrdemDeServicoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand("OS-404")));

        assertEquals("Ordem de servico nao encontrada para o numero informado.", exception.getMessage());
    }

    private OrdemDeServico novaOrdem(String numero) {
        UUID clienteId = UUID.fromString("61111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("71111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes()),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes()),
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
