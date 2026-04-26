package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class JpaOrdemDeServicoRepositoryIntegrationTest {

    @Autowired
    private JpaOrdemDeServicoRepository repository;

    @Autowired
    private SpringDataVeiculoRepository veiculoRepository;

    @Test
    void devePersistirBuscarPorNumeroEFiltros() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Marcos", "12345678901");
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF);
        Veiculo veiculo = veiculoRepository.save(new Veiculo(
                clienteId,
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        OrdemDeServico ordem = OrdemDeServico.abrir(null, "OS-2026-001", funcionario, cliente, veiculo);

        repository.salvar(ordem);

        OrdemDeServico encontrada = repository.buscarPorNumero("OS-2026-001").orElseThrow();
        assertEquals(StatusOrdemDeServico.OS_ABERTA, encontrada.getStatus());
        assertEquals("Maria", encontrada.getCliente().getNome());
        assertEquals("ABC1D23", encontrada.getVeiculo().getPlaca());

        List<OrdemDeServico> filtradas = repository.buscarPorFiltros(
                "OS-2026-001",
                "Maria",
                "ABC1D23",
                "12345678901");

        assertEquals(1, filtradas.size());
        assertEquals(1, repository.buscarTodas().size());
    }

    @Test
    void devePersistirDataDeEntregaQuandoOrdemForEntregue() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Larissa", "12312312399");
        Cliente cliente = Cliente.reconstituir(clienteId, "Claudio", "12312312399", TipoCliente.PF);
        Veiculo veiculo = veiculoRepository.save(new Veiculo(
                clienteId,
                "ENT1R23",
                "Volkswagen",
                "Polo",
                "Volkswagen",
                2024,
                116,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        OrdemDeServico ordem = OrdemDeServico.abrir(null, "OS-2026-003", funcionario, cliente, veiculo);
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();
        ordem.enviarParaOrcamento();
        ordem.finalizar();
        ordem.entregarAoCliente();

        repository.salvar(ordem);

        OrdemDeServico encontrada = repository.buscarPorNumero("OS-2026-003").orElseThrow();
        assertEquals(StatusOrdemDeServico.ENTREGUE, encontrada.getStatus());
        assertTrue(encontrada.getFinalizadaEm() != null);
        assertTrue(encontrada.getEntregueEm() != null);
    }

    @Test
    void deveExcluirOrdemDeServicoPorNumero() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Julia", "10987654321");
        Cliente cliente = Cliente.reconstituir(clienteId, "Carlos", "10987654321", TipoCliente.PF);
        Veiculo veiculo = veiculoRepository.save(new Veiculo(
                clienteId,
                "DEF2G34",
                "Honda",
                "Civic",
                "Honda",
                2023,
                155,
                "CVT",
                TipoCombustivel.GASOLINA));
        repository.salvar(OrdemDeServico.abrir(null, "OS-2026-002", funcionario, cliente, veiculo));

        assertTrue(repository.buscarPorNumero("OS-2026-002").isPresent());

        repository.excluirPorNumero("OS-2026-002");

        assertFalse(repository.buscarPorNumero("OS-2026-002").isPresent());
    }
}
