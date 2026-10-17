package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class TransicoesCicloDeVidaTest {

    @Test
    void deveSeguirCaminhoCanonicoAteFinalizacaoPorServicoConcluido() {
        OrdemDeServico ordem = novaOrdem();
        assertEquals(SituacaoOrdemDeServico.RECEBIDA, ordem.getSituacao());

        ordem.iniciarDiagnostico();
        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, ordem.getSituacao());

        ordem.concluirDiagnostico();
        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, ordem.getSituacao());

        ordem.enviarParaAprovacao();
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_APROVACAO, ordem.getSituacao());

        // Fase 4: a Saga leva a ordem a "Aguardando Pagamento" (pecas reservadas) e depois a fila de
        // execucao (pagamento confirmado).
        ordem.aguardarPagamento();
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_PAGAMENTO, ordem.getSituacao());
        ordem.confirmarPagamento();
        assertEquals(StatusOrdemDeServico.AGUARDANDO_EXECUCAO, ordem.getStatus());
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, ordem.getSituacao());

        ordem.iniciarExecucao();
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, ordem.getSituacao());

        ordem.concluirServico();
        assertEquals(SituacaoOrdemDeServico.FINALIZADA, ordem.getSituacao());
        assertEquals(MotivoEncerramento.SERVICO_CONCLUIDO, ordem.getMotivoEncerramento());
    }

    @Test
    void deveEncerrarComoRecusadoAoRecusarOrcamento() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();
        ordem.enviarParaAprovacao();

        ordem.recusarOrcamento();

        assertEquals(SituacaoOrdemDeServico.FINALIZADA, ordem.getSituacao());
        assertEquals(MotivoEncerramento.ORCAMENTO_RECUSADO, ordem.getMotivoEncerramento());
    }

    @Test
    void naoDeveTerMotivoEncerramentoAntesDeFinalizar() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        assertNull(ordem.getMotivoEncerramento());
    }

    @Test
    void deveRecusarEnviarParaAprovacaoSemDiagnosticoConcluido() {
        OrdemDeServico ordem = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::enviarParaAprovacao);
        assertEquals("Ordem de servico so pode ser enviada para aprovacao com diagnostico concluido", exception.getMessage());
    }

    @Test
    void deveRecusarIniciarExecucaoForaDeAguardandoExecucao() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::iniciarExecucao);
        assertEquals("Execucao so pode iniciar quando a ordem estiver aguardando execucao", exception.getMessage());
    }

    @Test
    void deveRecusarConfirmarPagamentoForaDeAguardandoPagamento() {
        OrdemDeServico ordem = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::confirmarPagamento);
        assertEquals("Pagamento so pode ser confirmado quando a ordem estiver aguardando pagamento", exception.getMessage());
    }

    @Test
    void deveCancelarPorPagamentoRecusadoOuExpirado() {
        for (MotivoEncerramento motivo : List.of(MotivoEncerramento.PAGAMENTO_RECUSADO, MotivoEncerramento.PAGAMENTO_EXPIRADO)) {
            OrdemDeServico ordem = aguardandoPagamento();

            ordem.cancelarPorPagamento(motivo);

            assertEquals(StatusOrdemDeServico.CANCELADA, ordem.getStatus());
            assertEquals(SituacaoOrdemDeServico.CANCELADA, ordem.getSituacao());
            assertEquals(motivo, ordem.getMotivoEncerramento());
            assertNotNull(ordem.getFinalizadaEm());
        }
    }

    @Test
    void deveRecusarCancelamentoPorPagamentoComMotivoErradoOuForaDeHora() {
        OrdemDeServico aguardando = aguardandoPagamento();
        assertThrows(IllegalArgumentException.class, () -> aguardando.cancelarPorPagamento(MotivoEncerramento.SERVICO_CONCLUIDO));
        OrdemDeServico aberta = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class,
                () -> aberta.cancelarPorPagamento(MotivoEncerramento.PAGAMENTO_RECUSADO));
        assertEquals("Ordem so pode ser cancelada por pagamento quando estiver aguardando pagamento", exception.getMessage());
    }

    @Test
    void deveCancelarAoAbortarAExecucao() {
        OrdemDeServico ordem = aguardandoPagamento();
        ordem.confirmarPagamento();
        ordem.iniciarExecucao();

        ordem.abortarExecucao();

        assertEquals(StatusOrdemDeServico.CANCELADA, ordem.getStatus());
        assertEquals(MotivoEncerramento.FALHA_EXECUCAO, ordem.getMotivoEncerramento());
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, novaOrdem()::abortarExecucao);
        assertEquals("Execucao so pode ser abortada quando estiver em andamento", exception.getMessage());
    }

    @Test
    void deveReabrirODiagnosticoSemAsPecasEMantendoOsServicos() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        ordem.registrarDiagnostico(List.of(new ServicoOrdem("Troca de oleo", new BigDecimal("120.00"))),
                List.of(new PecaPrevistaOrdem("peca-1", 1)));

        ordem.reabrirDiagnostico();

        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, ordem.getStatus());
        assertEquals(1, ordem.getServicos().size());
        assertTrue(ordem.getPecasPrevistas().isEmpty());
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, novaOrdem()::reabrirDiagnostico);
        assertEquals("Diagnostico so pode ser reaberto quando concluido", exception.getMessage());
    }

    @Test
    void diagnosticoReabertoEFechadoDeNovoSubstituiACopiaAnterior() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        ordem.registrarDiagnostico(List.of(new ServicoOrdem("Troca de oleo", new BigDecimal("120.00"))),
                List.of(new PecaPrevistaOrdem("peca-1", 1)));
        ordem.reabrirDiagnostico();

        ordem.registrarDiagnostico(List.of(new ServicoOrdem("Troca de oleo e filtro", new BigDecimal("180.00"))),
                List.of(new PecaPrevistaOrdem("peca-2", 2)));

        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO, ordem.getStatus());
        assertEquals(1, ordem.getServicos().size());
        assertEquals("Troca de oleo e filtro", ordem.getServicos().get(0).getDescricao());
        assertEquals(new BigDecimal("180.00"), ordem.getServicos().get(0).getValorMaoDeObra());
        assertEquals(1, ordem.getPecasPrevistas().size());
        assertEquals("peca-2", ordem.getPecasPrevistas().get(0).getPecaInsumoId());
    }

    @Test
    void registrarDiagnosticoExigeDiagnosticoEmAndamentoEAoMenosUmServico() {
        RegraDeNegocioException foraDeAndamento = assertThrows(RegraDeNegocioException.class, () -> novaOrdem()
                .registrarDiagnostico(List.of(new ServicoOrdem("Troca", BigDecimal.TEN)), List.of()));
        assertEquals("Diagnostico so pode ser concluido em andamento", foraDeAndamento.getMessage());

        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        RegraDeNegocioException semServico = assertThrows(RegraDeNegocioException.class,
                () -> ordem.registrarDiagnostico(List.of(), List.of()));
        assertEquals("O diagnostico precisa de ao menos um servico", semServico.getMessage());
        assertThrows(RegraDeNegocioException.class, () -> ordem.registrarDiagnostico(null, null));
        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, ordem.getStatus());
    }

    @Test
    void registrarDiagnosticoSemPecasGuardaSoOsServicos() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();

        ordem.registrarDiagnostico(List.of(new ServicoOrdem("Alinhamento", new BigDecimal("90.00"))), null);

        assertTrue(ordem.getPecasPrevistas().isEmpty());
        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO, ordem.getStatus());
    }

    private OrdemDeServico aguardandoPagamento() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();
        ordem.enviarParaAprovacao();
        ordem.aguardarPagamento();
        return ordem;
    }

    @Test
    void deveEncerrarComoExpiradoQuandoOPrazoDeDecisaoVence() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();
        ordem.enviarParaAprovacao();

        ordem.expirarOrcamento();

        assertEquals(SituacaoOrdemDeServico.FINALIZADA, ordem.getSituacao());
        assertEquals(MotivoEncerramento.ORCAMENTO_EXPIRADO, ordem.getMotivoEncerramento());
        assertNotNull(ordem.getFinalizadaEm());
    }

    @Test
    void deveRecusarExpirarOrcamentoForaDeAguardandoAprovacao() {
        OrdemDeServico ordem = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::expirarOrcamento);
        assertEquals("Orcamento so pode expirar quando a ordem estiver aguardando aprovacao", exception.getMessage());
    }

    @Test
    void deveRecusarConcluirServicoForaDeExecucao() {
        OrdemDeServico ordem = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::concluirServico);
        assertEquals("Servico so pode ser concluido quando estiver em execucao", exception.getMessage());
    }

    @Test
    void deveRecusarRecusarOrcamentoForaDeAguardandoAprovacao() {
        OrdemDeServico ordem = novaOrdem();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::recusarOrcamento);
        assertEquals("Orcamento so pode ser recusado quando a ordem estiver aguardando aprovacao", exception.getMessage());
    }

    @Test
    void deveMapearTodosOsStatusInternosParaSituacaoDeNegocio() {
        assertEquals(SituacaoOrdemDeServico.RECEBIDA, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.OS_ABERTA));
        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO));
        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO));
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_APROVACAO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.ORCAMENTO_GERADO));
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_APROVACAO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.AGUARDANDO_APROVACAO));
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.AGUARDANDO_EXECUCAO));
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.SERVICO_EM_ANDAMENTO));
        assertEquals(SituacaoOrdemDeServico.FINALIZADA, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.OS_FINALIZADA));
        assertEquals(SituacaoOrdemDeServico.ENTREGUE, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.ENTREGUE));
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_PAGAMENTO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO));
        assertEquals(SituacaoOrdemDeServico.CANCELADA, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.CANCELADA));
        assertEquals("Aguardando Pagamento", SituacaoOrdemDeServico.AGUARDANDO_PAGAMENTO.getDescricao());
        assertEquals("Cancelada", SituacaoOrdemDeServico.CANCELADA.getDescricao());
        assertEquals("Aguardando Aprovação", SituacaoOrdemDeServico.AGUARDANDO_APROVACAO.getDescricao());
    }

    @Test
    void naoDeveExistirStatusOrfaoNoVocabulario() {
        assertEquals(11, StatusOrdemDeServico.values().length);
        for (StatusOrdemDeServico status : StatusOrdemDeServico.values()) {
            assertNotNull(SituacaoOrdemDeServico.fromStatus(status));
        }
    }

    private static OrdemDeServico novaOrdem() {
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        UUID veiculoId = UUID.fromString("51111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                null,
                "OS-CICLO01",
                Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"),
                Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        veiculoId, clienteId, "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
    }
}
