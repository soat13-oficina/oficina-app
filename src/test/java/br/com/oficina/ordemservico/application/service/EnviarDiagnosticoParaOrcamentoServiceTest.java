package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.saga.application.PassosDaSaga;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

/**
 * Fase 4: o envio para orcamento so valida e pede o orcamento a Saga (comando GerarOrcamento). A OS
 * continua com o diagnostico concluido ate o OrcamentoGerado chegar (coberto no teste do orquestrador).
 */
class EnviarDiagnosticoParaOrcamentoServiceTest {

    private static final UUID CLIENTE_ID = UUID.fromString("61111111-1111-1111-1111-111111111111");

    private final TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
    private final List<PassosDaSaga.DadosOrcamento> pedidos = new ArrayList<>();
    private final EnviarDiagnosticoParaOrcamentoService service =
            new EnviarDiagnosticoParaOrcamentoService(ordemRepository, sagaQueRegistra(pedidos));

    @Test
    void pedeOOrcamentoASagaSemMudarAOrdem() {
        prepararOrdemEmDiagnosticoConcluido("OS-001");

        service.enviarDiagnosticoParaOrcamento(requisicao("OS-001", new BigDecimal("200.00"), BigDecimal.ZERO, LocalDateTime.of(2030, 1, 1, 0, 0)));

        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO, ordemRepository.buscarPorNumero("OS-001").orElseThrow().getStatus());
        assertEquals(List.of(new PassosDaSaga.DadosOrcamento(new BigDecimal("200.00"), BigDecimal.ZERO, LocalDateTime.of(2030, 1, 1, 0, 0), "obs")), pedidos);
    }

    @Test
    void validadeEOpcional() {
        prepararOrdemEmDiagnosticoConcluido("OS-002");

        service.enviarDiagnosticoParaOrcamento(requisicao("OS-002", new BigDecimal("200.00"), null, null));

        assertEquals(1, pedidos.size());
    }

    @Test
    void ordemInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-404", BigDecimal.TEN, null, null)));
    }

    @Test
    void exigeDiagnosticoConcluidoComServicoOuPeca() {
        OrdemDeServico aberta = novaOrdem("OS-003");
        ordemRepository.salvar(aberta);
        OrdemDeServico semDados = novaOrdem("OS-004");
        semDados.iniciarDiagnostico();
        semDados.concluirDiagnostico();
        ordemRepository.salvar(semDados);

        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-003", BigDecimal.TEN, null, null)));
        RegraDeNegocioException semDiagnostico = assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-004", BigDecimal.TEN, null, null)));
        assertTrue(semDiagnostico.getMessage().startsWith("Diagnostico nao fechado"));
        assertTrue(pedidos.isEmpty());
    }

    @Test
    void recusaDadoFinanceiroInvalidoAntesDeChamarOBilling() {
        prepararOrdemEmDiagnosticoConcluido("OS-005");

        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-005", null, null, null)));
        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-005", new BigDecimal("-1"), null, null)));
        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-005", BigDecimal.TEN, new BigDecimal("-1"), null)));
        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-005", BigDecimal.TEN, null, LocalDateTime.now().minusDays(1))));
        assertTrue(pedidos.isEmpty());
    }

    private static PassosDaSaga sagaQueRegistra(List<PassosDaSaga.DadosOrcamento> pedidos) {
        return new PassosDaSaga() {
            @Override
            public void iniciar(UUID ordemDeServicoId) {
            }

            @Override
            public void solicitarOrcamento(UUID ordemDeServicoId, DadosOrcamento dados) {
                pedidos.add(dados);
            }

            @Override
            public void execucaoIniciada(UUID ordemDeServicoId) {
            }

            @Override
            public void execucaoConcluida(UUID ordemDeServicoId) {
            }

            @Override
            public void execucaoFalhou(UUID ordemDeServicoId, String motivo) {
            }
        };
    }

    private void prepararOrdemEmDiagnosticoConcluido(String numero) {
        OrdemDeServico ordem = novaOrdem(numero);
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico("Servico de diagnostico", List.of());
        ordemRepository.salvar(ordem);
    }

    private static EnviarDiagnosticoParaOrcamentoRequest requisicao(String numero, BigDecimal maoDeObra, BigDecimal desconto, LocalDateTime validade) {
        return new EnviarDiagnosticoParaOrcamentoRequest(numero, maoDeObra, desconto, validade, "obs");
    }

    private static OrdemDeServico novaOrdem(String numero) {
        UUID funcionarioId = UUID.fromString("71111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes()),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes()),
                        CLIENTE_ID,
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
