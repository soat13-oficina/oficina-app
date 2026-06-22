package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.ServicoOrdem;
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
    void deveBuscarOrdensComExecucaoFinalizada() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Larissa", "12312312399");
        Cliente cliente = Cliente.reconstituir(clienteId, "Claudio", "12312312399", TipoCliente.PF);
        Veiculo veiculo1 = veiculoRepository.save(new Veiculo(
                clienteId,
                "MET1R23",
                "Volkswagen",
                "Polo",
                "Volkswagen",
                2024,
                116,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        Veiculo veiculo2 = veiculoRepository.save(new Veiculo(
                clienteId,
                "MET2R23",
                "Volkswagen",
                "Virtus",
                "Volkswagen",
                2024,
                128,
                "AUTOMATICO",
                TipoCombustivel.FLEX));

        repository.salvar(OrdemDeServico.reconstituir(
                null,
                "OS-2026-004",
                funcionario,
                cliente,
                veiculo1,
                StatusOrdemDeServico.OS_FINALIZADA,
                LocalDateTime.of(2030, 1, 1, 8, 0),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                null));
        repository.salvar(OrdemDeServico.reconstituir(
                null,
                "OS-2026-005",
                funcionario,
                cliente,
                veiculo2,
                StatusOrdemDeServico.ENTREGUE,
                LocalDateTime.of(2030, 1, 1, 9, 0),
                LocalDateTime.of(2030, 1, 1, 12, 0),
                LocalDateTime.of(2030, 1, 1, 13, 0)));
        repository.salvar(OrdemDeServico.abrir(null, "OS-2026-006", funcionario, cliente, veiculo2));

        List<OrdemDeServico> ordensFinalizadas = repository.buscarOrdensComExecucaoFinalizada();

        assertEquals(2, ordensFinalizadas.size());
    }

    @Test
    void devePersistirServicosEPecasNaOrdemDeServico() {
        UUID clienteId = UUID.randomUUID();
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Pedro", "55566677788");
        Cliente cliente = Cliente.reconstituir(clienteId, "Lucia", "55566677788", TipoCliente.PF);
        Veiculo veiculo = veiculoRepository.save(new Veiculo(
                clienteId, "SVC1A23", "Honda", "Fit", "Honda", 2022, 120, "AUTOMATICO", TipoCombustivel.FLEX));

        OrdemDeServico ordem = OrdemDeServico.abrir(
                null, "OS-SERVICOS-001", funcionario, cliente, veiculo,
                List.of(new ServicoOrdem("Troca de filtro", new java.math.BigDecimal("80.00"))),
                List.of(new PecaPrevistaOrdem("peca-insumo-filtro-001", 2)));

        repository.salvar(ordem);

        OrdemDeServico encontrada = repository.buscarPorNumero("OS-SERVICOS-001").orElseThrow();
        assertEquals(1, encontrada.getServicos().size());
        assertEquals("Troca de filtro", encontrada.getServicos().get(0).getDescricao());
        assertEquals(1, encontrada.getPecasPrevistas().size());
        assertEquals("peca-insumo-filtro-001", encontrada.getPecasPrevistas().get(0).getPecaInsumoId());
        assertEquals(2, encontrada.getPecasPrevistas().get(0).getQuantidade());
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
