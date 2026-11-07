package br.com.oficina.contratos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.infrastructure.mensageria.OutboxMensagemEntity;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;
import br.com.oficina.support.contratos.ContratosDeMensagens;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Teste de contrato do produtor (ADR 0005): roda os caminhos da Saga e valida cada mensagem que
 * passou pelo outbox contra o JSON Schema do seu {@code type} em {@code docs/contratos/schemas}:
 * <ul>
 *   <li>comandos do orquestrador;</li>
 *   <li>eventos do {@code BillingSimulado} e do {@code ExecutionSimulado}: os dubles tambem tem de seguir
 *       o contrato, senao os testes do OS provariam a Saga contra respostas que os servicos de verdade
 *       nao mandam. Billing e Execution validam as proprias mensagens nos repositorios deles.</li>
 * </ul>
 */
@SpringBootTest
class ContratoDasMensagensDaSagaTest {

    /** Tudo o que o OS produz: os comandos do orquestrador. */
    private static final Set<String> PRODUZIDAS = Set.of(
            MensagensSaga.ENFILEIRAR_DIAGNOSTICO, MensagensSaga.REABRIR_DIAGNOSTICO, MensagensSaga.GERAR_ORCAMENTO,
            MensagensSaga.RESERVAR_PECAS, MensagensSaga.SOLICITAR_PAGAMENTO, MensagensSaga.ENFILEIRAR_EXECUCAO,
            MensagensSaga.REVERTER_APROVACAO, MensagensSaga.CANCELAR_ORCAMENTO, MensagensSaga.LIBERAR_RESERVA,
            MensagensSaga.CANCELAR_EXECUCAO, MensagensSaga.ESTORNAR_PAGAMENTO);

    /** Respostas e acoes do mecanico na Execution, aqui produzidas pelo ExecutionSimulado. */
    private static final Set<String> DO_EXECUTION_SIMULADO = Set.of(
            MensagensSaga.DIAGNOSTICO_ENFILEIRADO, MensagensSaga.DIAGNOSTICO_INICIADO, MensagensSaga.DIAGNOSTICO_CONCLUIDO,
            MensagensSaga.DIAGNOSTICO_REABERTO, MensagensSaga.PECAS_RESERVADAS, MensagensSaga.RESERVA_FALHOU,
            MensagensSaga.EXECUCAO_ENFILEIRADA, MensagensSaga.EXECUCAO_INICIADA, MensagensSaga.EXECUCAO_CONCLUIDA,
            MensagensSaga.EXECUCAO_FALHOU, MensagensSaga.RESERVA_LIBERADA, MensagensSaga.EXECUCAO_CANCELADA);

    /** Respostas do Billing, aqui produzidas pelo BillingSimulado. */
    private static final Set<String> DO_BILLING_SIMULADO = Set.of(
            MensagensSaga.ORCAMENTO_GERADO, MensagensSaga.ORCAMENTO_FALHOU, MensagensSaga.ORCAMENTO_APROVADO,
            MensagensSaga.ORCAMENTO_REJEITADO, MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.ORCAMENTO_CANCELADO,
            MensagensSaga.PAGAMENTO_SOLICITADO, MensagensSaga.PAGAMENTO_CONFIRMADO, MensagensSaga.PAGAMENTO_RECUSADO,
            MensagensSaga.PAGAMENTO_EXPIRADO, MensagensSaga.PAGAMENTO_ESTORNADO, MensagensSaga.ESTORNO_FALHOU);

    private static final ContratosDeMensagens CONTRATOS = new ContratosDeMensagens();

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private SpringDataOutboxMensagemRepository outbox;

    @Autowired
    private OrquestradorSagaOrdemServico orquestrador;

    @Autowired
    private PublicadorMensagens publicador;

    @Autowired
    private FabricaEnvelopes fabrica;

    @Autowired
    private TransactionTemplate transacao;

    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void preparar() {
        fluxo = new FluxoOficinaHttp(MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build(), contexto);
        fluxo.limpar();
    }

    @AfterEach
    void limpar() {
        fluxo.limpar();
    }

    @Test
    void todaMensagemQueOOsEOsServicosSimuladosProduzemRespeitaOContrato() throws Exception {
        caminhoFelizAteAConclusao();
        rejeicao();
        estoqueInsuficiente();
        pagamentoRecusado();
        pagamentoExpirado();
        execucaoAbortada();
        pecaForaDoCatalogo();
        estornoDePagamentoInexistente();

        List<OutboxMensagemEntity> mensagens = outbox.findAll();
        List<String> erros = new ArrayList<>();
        Set<String> tipos = new TreeSet<>();
        for (OutboxMensagemEntity mensagem : mensagens) {
            tipos.add(mensagem.getTipo());
            erros.addAll(CONTRATOS.validar(mensagem.getPayload()));
        }

        assertThat(erros).as("mensagens fora do contrato").isEmpty();
        assertThat(tipos).as("todo tipo produzido foi exercitado")
                .containsAll(PRODUZIDAS).containsAll(DO_BILLING_SIMULADO).containsAll(DO_EXECUTION_SIMULADO);
        assertThat(CONTRATOS.tipos()).as("todo tipo produzido tem schema")
                .containsAll(PRODUZIDAS).containsAll(DO_BILLING_SIMULADO).containsAll(DO_EXECUTION_SIMULADO);
    }

    private String aguardandoPagamento(String placa, int estoque) throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro " + placa, estoque);
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticarComPeca(numero, "Troca do filtro", pecaId, 2);
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();
        return numero;
    }

    private void caminhoFelizAteAConclusao() throws Exception {
        String numero = aguardandoPagamento("CTR1A11", 5);
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "APROVADO");
        fluxo.processarMensagens();
        fluxo.iniciarExecucao(numero);
        fluxo.concluirExecucao(numero);
    }

    private void rejeicao() throws Exception {
        String numero = fluxo.abrirOrdem("CTR2A22");
        fluxo.diagnosticar(numero, "Troca de oleo");
        fluxo.clienteRejeita(fluxo.enviarParaOrcamento(numero));
        fluxo.processarMensagens();
    }

    private void estoqueInsuficiente() throws Exception {
        aguardandoPagamento("CTR3A33", 1);
    }

    private void pagamentoRecusado() throws Exception {
        String numero = aguardandoPagamento("CTR4A44", 5);
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "RECUSADO");
        fluxo.processarMensagens();
    }

    private void pagamentoExpirado() throws Exception {
        String numero = aguardandoPagamento("CTR5A55", 5);
        fluxo.expirarPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento());
        fluxo.processarMensagens();
    }

    private void execucaoAbortada() throws Exception {
        String numero = aguardandoPagamento("CTR6A66", 5);
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "APROVADO");
        fluxo.processarMensagens();
        fluxo.iniciarExecucao(numero);
        fluxo.abortarExecucao(numero, "Peca incompativel");
    }

    private void pecaForaDoCatalogo() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Some", 5);
        String numero = fluxo.abrirOrdem("CTR7A77");
        fluxo.iniciarDiagnostico(numero);
        fluxo.catalogo().remover(pecaId); // sai do catalogo entre o diagnostico e o orcamento
        fluxo.concluirDiagnostico(numero, "Troca", pecaId, 1);
        orquestrador.processarPrazosVencidos(LocalDateTime.now());
    }

    /** O Billing simulado responde EstornoFalhou quando o pagamento nao existe. */
    private void estornoDePagamentoInexistente() throws Exception {
        String numero = fluxo.abrirOrdem("CTR8A88");
        UUID ordemId = fluxo.ordem(numero).getId();
        transacao.executeWithoutResult(s -> publicador.publicar(Filas.BILLING_COMMANDS, fabrica.novo(
                MensagensSaga.ESTORNAR_PAGAMENTO, UUID.randomUUID(), ordemId,
                Map.of("orcamentoId", UUID.randomUUID().toString(), "pagamentoId", UUID.randomUUID().toString(),
                        "motivo", "FALHA_EXECUCAO"))));
        fluxo.processarMensagens();
    }
}
