package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class ConsultarOrdensDeServicoServiceTest {

    @Test
    void deveConsultarOrdensDeServicoComFiltros() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-001", "Marina", "12345678901", "AAA1A11"));
        repository.salvar(novaOrdem("OS-002", "Bianca", "99999999999", "BBB2B22"));
        ConsultarOrdensDeServicoService service = new ConsultarOrdensDeServicoService(repository);

        List<OrdemDeServico> resultado = service.consultarOrdensDeServico(
                new ConsultarOrdensDeServicoQuery("OS-001", "Marina", "AAA1A11", "12345678901"));

        assertEquals(1, resultado.size());
        assertEquals("OS-001", resultado.get(0).getNumeroOrdemServico());
    }

    @Test
    void deveRetornarTodasAsOrdensQuandoNaoHouverFiltros() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-001", "Marina", "12345678901", "AAA1A11"));
        repository.salvar(novaOrdem("OS-002", "Bianca", "99999999999", "BBB2B22"));
        ConsultarOrdensDeServicoService service = new ConsultarOrdensDeServicoService(repository);

        List<OrdemDeServico> resultado = service.consultarOrdensDeServico(
                new ConsultarOrdensDeServicoQuery(null, null, null, null));

        assertEquals(2, resultado.size());
    }

    private OrdemDeServico novaOrdem(String numero, String nomeCliente, String documento, String placa) {
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes(StandardCharsets.UTF_8)),
                numero,
                new Funcionario("func-1", "Joao", null),
                Cliente.reconstituir(UUID.nameUUIDFromBytes(("cliente-" + numero).getBytes(StandardCharsets.UTF_8)), nomeCliente, documento, TipoCliente.PF),
                new Veiculo(
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
