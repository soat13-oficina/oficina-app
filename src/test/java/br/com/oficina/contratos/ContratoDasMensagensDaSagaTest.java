package br.com.oficina.contratos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.oficina.pagamento.application.ResultadoPagamentoService;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.support.contratos.ContratosDeMensagens;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Teste de contrato do produtor (ADR 0005): roda os caminhos da Saga dentro do monolito e valida
 * cada mensagem que passou pelo outbox - comandos do orquestrador e eventos dos participantes que
 * ainda moram aqui - contra o JSON Schema do seu {@code type} em {@code docs/contratos/schemas}.
 * Quando Billing e Execution sairem, cada um valida as proprias mensagens no seu repositorio.
 */
@SpringBootTest
class ContratoDasMensagensDaSagaTest {

    /** Tudo o que o monolito produz hoje. Diagnostico e execucao ainda nao sao mensagens (Passo 4). */
    private static final Set<String> PRODUZIDAS = Set.of(
            MensagensSaga.GERAR_ORCAMENTO, MensagensSaga.RESERVAR_PECAS, MensagensSaga.SOLICITAR_PAGAMENTO,
            MensagensSaga.REVERTER_APROVACAO, MensagensSaga.CANCELAR_ORCAMENTO, MensagensSaga.LIBERAR_RESERVA,
            MensagensSaga.ESTORNAR_PAGAMENTO,
            MensagensSaga.ORCAMENTO_GERADO, MensagensSaga.ORCAMENTO_FALHOU, MensagensSaga.ORCAMENTO_APROVADO,
            MensagensSaga.ORCAMENTO_REJEITADO, MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.ORCAMENTO_CANCELADO,
            MensagensSaga.PECAS_RESERVADAS, MensagensSaga.RESERVA_FALHOU, MensagensSaga.RESERVA_LIBERADA,
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
    private ResultadoPagamentoService resultadoPagamento;

    @Autowired
    private SpringDataPecaInsumoRepository pecas;

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
    void todaMensagemQueOMonolitoProduzRespeitaOContrato() throws Exception {
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
        assertThat(tipos).as("todo tipo produzido foi exercitado").containsAll(PRODUZIDAS);
        assertThat(CONTRATOS.tipos()).as("todo tipo produzido tem schema").containsAll(PRODUZIDAS);
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
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "APROVADO").andExpect(status().isOk());
        fluxo.processarMensagens();
        fluxo.acao(numero, "/execucao/iniciar");
        fluxo.acao(numero, "/servico/concluir");
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
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "RECUSADO").andExpect(status().isOk());
        fluxo.processarMensagens();
    }

    private void pagamentoExpirado() throws Exception {
        aguardandoPagamento("CTR5A55", 5);
        resultadoPagamento.expirarVencidos(LocalDateTime.now().plusDays(2));
        fluxo.processarMensagens();
    }

    private void execucaoAbortada() throws Exception {
        String numero = aguardandoPagamento("CTR6A66", 5);
        fluxo.simularPagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento(), "APROVADO").andExpect(status().isOk());
        fluxo.processarMensagens();
        fluxo.acao(numero, "/execucao/iniciar");
        fluxo.abortarExecucao(numero, "Peca incompativel");
        fluxo.processarMensagens();
    }

    private void pecaForaDoCatalogo() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Some", 5);
        String numero = fluxo.abrirOrdem("CTR7A77");
        fluxo.diagnosticarComPeca(numero, "Troca", pecaId, 1);
        pecas.deleteById(pecaId);
        fluxo.solicitarOrcamento(numero);
        fluxo.processarMensagens();
        orquestrador.processarPrazosVencidos(LocalDateTime.now());
    }

    /** O simulador sempre estorna; EstornoFalhou sai quando o pagamento nao existe. */
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
