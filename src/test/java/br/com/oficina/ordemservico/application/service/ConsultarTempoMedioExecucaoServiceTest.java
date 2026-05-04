package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase.TempoMedioExecucao;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class ConsultarTempoMedioExecucaoServiceTest {

    @Test
    void deveCalcularTempoMedioDeExecucaoDasOrdensFinalizadas() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdemFinalizada("OS-001", LocalDateTime.of(2030, 1, 1, 8, 0), LocalDateTime.of(2030, 1, 1, 10, 0), StatusOrdemDeServico.OS_FINALIZADA));
        repository.salvar(novaOrdemFinalizada("OS-002", LocalDateTime.of(2030, 1, 1, 9, 0), LocalDateTime.of(2030, 1, 1, 12, 0), StatusOrdemDeServico.ENTREGUE));
        repository.salvar(novaOrdemAberta("OS-003"));

        ConsultarTempoMedioExecucaoService service = new ConsultarTempoMedioExecucaoService(repository);

        TempoMedioExecucao resultado = service.consultarTempoMedioExecucao();

        assertEquals(9000, resultado.tempoMedioExecucaoEmSegundos());
        assertEquals("2 horas e 30 minutos", resultado.tempoMedioExecucaoFormatado());
        assertEquals(2, resultado.quantidadeOrdensConsideradas());
    }

    @Test
    void deveRetornarZeroQuandoNaoExistiremOrdensFinalizadas() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdemAberta("OS-010"));

        ConsultarTempoMedioExecucaoService service = new ConsultarTempoMedioExecucaoService(repository);

        TempoMedioExecucao resultado = service.consultarTempoMedioExecucao();

        assertEquals(0, resultado.tempoMedioExecucaoEmSegundos());
        assertEquals("0 minutos", resultado.tempoMedioExecucaoFormatado());
        assertEquals(0, resultado.quantidadeOrdensConsideradas());
    }

    private OrdemDeServico novaOrdemAberta(String numero) {
        UUID clienteId = UUID.nameUUIDFromBytes(("cliente-" + numero).getBytes(StandardCharsets.UTF_8));
        UUID funcionarioId = UUID.nameUUIDFromBytes(("funcionario-" + numero).getBytes(StandardCharsets.UTF_8));
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes(StandardCharsets.UTF_8)),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes(StandardCharsets.UTF_8)),
                        clienteId,
                        "AAA1A11",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
    }

    private OrdemDeServico novaOrdemFinalizada(
            String numero,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            StatusOrdemDeServico status) {
        UUID clienteId = UUID.nameUUIDFromBytes(("cliente-" + numero).getBytes(StandardCharsets.UTF_8));
        UUID funcionarioId = UUID.nameUUIDFromBytes(("funcionario-" + numero).getBytes(StandardCharsets.UTF_8));
        UUID veiculoId = UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes(StandardCharsets.UTF_8));
        LocalDateTime entregueEm = status == StatusOrdemDeServico.ENTREGUE ? finalizadaEm.plusHours(1) : null;
        return OrdemDeServico.reconstituir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes(StandardCharsets.UTF_8)),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF),
                Veiculo.reconstituir(
                        veiculoId,
                        clienteId,
                        "BBB2B22",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX),
                status,
                iniciadaEm,
                finalizadaEm,
                entregueEm);
    }
}
