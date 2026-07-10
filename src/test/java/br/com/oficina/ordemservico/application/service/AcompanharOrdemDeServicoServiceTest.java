package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class AcompanharOrdemDeServicoServiceTest {

    @Test
    void deveRetornarOrdemQuandoDocumentoClienteCorresponder() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-CLIENTE-001", "Maria", "12345678909", "AAA1A11"));
        AcompanharOrdemDeServicoService service = new AcompanharOrdemDeServicoService(repository);

        OrdemDeServico ordemDeServico = service.acompanhar(
                new AcompanharOrdemDeServicoQuery("OS-CLIENTE-001", "12345678909"));

        assertEquals("OS-CLIENTE-001", ordemDeServico.getNumeroOrdemServico());
        assertEquals("Maria", ordemDeServico.getCliente().getNome());
    }

    @Test
    void deveFalharQuandoOrdemNaoExistirOuNaoPertencerAoCliente() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-CLIENTE-001", "Maria", "12345678909", "AAA1A11"));
        AcompanharOrdemDeServicoService service = new AcompanharOrdemDeServicoService(repository);

        RecursoNaoEncontradoException ordemNaoEncontrada = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.acompanhar(new AcompanharOrdemDeServicoQuery("OS-404", "12345678909")));
        assertEquals("Ordem de servico nao encontrada", ordemNaoEncontrada.getMessage());

        RecursoNaoEncontradoException clienteInvalido = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.acompanhar(new AcompanharOrdemDeServicoQuery("OS-CLIENTE-001", "20100000053")));
        assertEquals("Ordem de servico nao encontrada", clienteInvalido.getMessage());
    }

    private OrdemDeServico novaOrdem(String numero, String nomeCliente, String documento, String placa) {
        UUID clienteId = UUID.nameUUIDFromBytes(("cliente-" + numero).getBytes(StandardCharsets.UTF_8));
        UUID funcionarioId = UUID.nameUUIDFromBytes(("funcionario-" + numero).getBytes(StandardCharsets.UTF_8));
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes(StandardCharsets.UTF_8)),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, nomeCliente, documento, TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes(StandardCharsets.UTF_8)),
                        clienteId,
                        placa,
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
    }
}
