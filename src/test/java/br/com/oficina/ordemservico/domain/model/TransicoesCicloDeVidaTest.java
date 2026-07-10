package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void deveRecusarIniciarExecucaoForaDeAguardandoAprovacao() {
        OrdemDeServico ordem = novaOrdem();
        ordem.iniciarDiagnostico();
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, ordem::iniciarExecucao);
        assertEquals("Execucao so pode iniciar quando a ordem estiver aguardando aprovacao", exception.getMessage());
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
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.SERVICO_EM_ANDAMENTO));
        assertEquals(SituacaoOrdemDeServico.FINALIZADA, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.OS_FINALIZADA));
        assertEquals(SituacaoOrdemDeServico.ENTREGUE, SituacaoOrdemDeServico.fromStatus(StatusOrdemDeServico.ENTREGUE));
        assertEquals("Aguardando Aprovação", SituacaoOrdemDeServico.AGUARDANDO_APROVACAO.getDescricao());
    }

    @Test
    void naoDeveExistirStatusOrfaoNoVocabulario() {
        assertEquals(8, StatusOrdemDeServico.values().length);
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
