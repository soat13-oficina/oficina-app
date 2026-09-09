package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

/**
 * Contrato de {@code situacaoAlteradaEm} (migration V19): o relógio segue a SITUAÇÃO de negócio,
 * não o status interno. É esse invariante que sustenta o "tempo médio de execução por status".
 */
class OrdemDeServicoSituacaoAlteradaEmTest {

    private static final UUID CLIENTE_ID = UUID.fromString("81111111-1111-1111-1111-111111111111");
    private static final UUID VEICULO_ID = UUID.fromString("82222222-2222-2222-2222-222222222222");
    private static final UUID FUNCIONARIO_ID = UUID.fromString("83333333-3333-3333-3333-333333333333");

    @Test
    void abrirOrdemJaIniciaORelogioDaSituacao() {
        OrdemDeServico ordem = abrir();

        assertEquals(SituacaoOrdemDeServico.RECEBIDA, ordem.getSituacao());
        assertNotNull(ordem.getSituacaoAlteradaEm());
    }

    @Test
    void deveAvancarORelogioQuandoASituacaoMuda() {
        OrdemDeServico ordem = abrir();
        LocalDateTime aoAbrir = ordem.getSituacaoAlteradaEm();

        ordem.iniciarDiagnostico();

        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, ordem.getSituacao());
        assertTrue(
                !ordem.getSituacaoAlteradaEm().isBefore(aoAbrir),
                "entrar em Diagnostico deve reposicionar o marco da situacao");
    }

    @Test
    void naoDeveReiniciarORelogioEntreDoisStatusDaMesmaSituacao() {
        // DIAGNOSTICO_EM_ANDAMENTO e DIAGNOSTICO_CONCLUIDO são status diferentes, mas ambos são
        // a situação "Diagnóstico": zerar o contador aqui partiria a etapa em dois pedaços e o
        // tempo médio de Diagnóstico sairia pela metade.
        OrdemDeServico ordem = abrir();
        ordem.iniciarDiagnostico();
        LocalDateTime aoEntrarEmDiagnostico = ordem.getSituacaoAlteradaEm();

        ordem.concluirDiagnostico();

        assertEquals(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO, ordem.getStatus());
        assertEquals(SituacaoOrdemDeServico.DIAGNOSTICO, ordem.getSituacao());
        assertEquals(aoEntrarEmDiagnostico, ordem.getSituacaoAlteradaEm());
    }

    @Test
    void deveAvancarORelogioEmCadaTrocaDeSituacaoAteAEntrega() {
        OrdemDeServico ordem = abrir();
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();

        ordem.enviarParaAprovacao();
        LocalDateTime aoAguardarAprovacao = ordem.getSituacaoAlteradaEm();
        assertEquals(SituacaoOrdemDeServico.AGUARDANDO_APROVACAO, ordem.getSituacao());

        ordem.iniciarExecucao();
        LocalDateTime aoExecutar = ordem.getSituacaoAlteradaEm();
        assertEquals(SituacaoOrdemDeServico.EXECUCAO, ordem.getSituacao());
        assertTrue(!aoExecutar.isBefore(aoAguardarAprovacao));

        ordem.concluirServico();
        LocalDateTime aoFinalizar = ordem.getSituacaoAlteradaEm();
        assertEquals(SituacaoOrdemDeServico.FINALIZADA, ordem.getSituacao());
        assertTrue(!aoFinalizar.isBefore(aoExecutar));

        ordem.entregarAoCliente();
        assertEquals(SituacaoOrdemDeServico.ENTREGUE, ordem.getSituacao());
        assertTrue(!ordem.getSituacaoAlteradaEm().isBefore(aoFinalizar));
    }

    @Test
    void finalizarAPartirDeOrcamentoGeradoTambemMoveORelogio() {
        // Caminho que ninguém instrumentava: ORCAMENTO_GERADO -> OS_FINALIZADA.
        OrdemDeServico ordem = abrir();
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();
        ordem.enviarParaOrcamento();
        LocalDateTime aoGerarOrcamento = ordem.getSituacaoAlteradaEm();

        ordem.finalizar();

        assertEquals(SituacaoOrdemDeServico.FINALIZADA, ordem.getSituacao());
        assertTrue(!ordem.getSituacaoAlteradaEm().isBefore(aoGerarOrcamento));
    }

    private static OrdemDeServico abrir() {
        return OrdemDeServico.abrir(
                null,
                "OS-0001",
                Funcionario.reconstituir(FUNCIONARIO_ID, "Joao", "12345678909"),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        VEICULO_ID,
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
