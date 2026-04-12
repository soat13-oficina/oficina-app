package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
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
        repository.salvar(novaOrdem("OS-001", "Marina", "123", "AAA1A11"));
        repository.salvar(novaOrdem("OS-002", "Bianca", "999", "BBB2B22"));
        ConsultarOrdensDeServicoService service = new ConsultarOrdensDeServicoService(repository);

        List<OrdemDeServico> resultado = service.consultarOrdensDeServico(
                new ConsultarOrdensDeServicoQuery("OS-001", "Marina", "AAA1A11", "123"));

        assertEquals(1, resultado.size());
        assertEquals("OS-001", resultado.get(0).getNumeroOrdemServico());
    }

    @Test
    void deveRetornarTodasAsOrdensQuandoNaoHouverFiltros() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-001", "Marina", "123", "AAA1A11"));
        repository.salvar(novaOrdem("OS-002", "Bianca", "999", "BBB2B22"));
        ConsultarOrdensDeServicoService service = new ConsultarOrdensDeServicoService(repository);

        List<OrdemDeServico> resultado = service.consultarOrdensDeServico(
                new ConsultarOrdensDeServicoQuery(null, null, null, null));

        assertEquals(2, resultado.size());
    }

    private OrdemDeServico novaOrdem(String numero, String nomeCliente, String cpf, String placa) {
        return OrdemDeServico.abrir(
                "id-" + numero,
                numero,
                new Funcionario("func-1", "Joao", null),
                new Cliente("cliente-" + numero, nomeCliente, cpf),
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
