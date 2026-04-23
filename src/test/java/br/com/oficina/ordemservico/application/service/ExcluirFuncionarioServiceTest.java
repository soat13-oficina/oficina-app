package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestFuncionarioRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class ExcluirFuncionarioServiceTest {

    @Test
    void deveExcluirFuncionarioExistente() {
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678901"));
        ExcluirFuncionarioService service = new ExcluirFuncionarioService(
                funcionarioRepository, new TestOrdemDeServicoRepository());

        service.excluirFuncionario(new ExcluirFuncionarioCommand(funcionarioId));

        assertTrue(funcionarioRepository.buscarPorId(funcionarioId).isEmpty());
    }

    @Test
    void deveFalharAoExcluirFuncionarioInexistente() {
        ExcluirFuncionarioService service = new ExcluirFuncionarioService(
                new TestFuncionarioRepository(), new TestOrdemDeServicoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirFuncionario(new ExcluirFuncionarioCommand(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))));

        assertEquals("Funcionario nao encontrado para o identificador informado.", exception.getMessage());
    }

    @Test
    void deveFalharAoExcluirFuncionarioVinculadoAOrdemDeServico() {
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();

        UUID funcionarioId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        Funcionario funcionario = Funcionario.reconstituir(funcionarioId, "Mecanico Vinculado", null);
        funcionarioRepository.salvar(funcionario);

        Cliente cliente = Cliente.reconstituir(UUID.randomUUID(), "Cliente Teste", "12345678901", TipoCliente.PF);
        Veiculo veiculo = Veiculo.reconstituir(UUID.randomUUID(), cliente.getId(), "ABC1234",
                "Toyota", "Corolla", "Toyota", 2020, 132, "AUTOMATICO", TipoCombustivel.FLEX);
        ordemDeServicoRepository.salvar(
                OrdemDeServico.abrir(UUID.randomUUID(), "OS-TEST01", funcionario, cliente, veiculo));

        ExcluirFuncionarioService service = new ExcluirFuncionarioService(
                funcionarioRepository, ordemDeServicoRepository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.excluirFuncionario(new ExcluirFuncionarioCommand(funcionarioId)));

        assertEquals("Nao e possivel excluir funcionario vinculado a ordens de servico.", exception.getMessage());
    }
}
