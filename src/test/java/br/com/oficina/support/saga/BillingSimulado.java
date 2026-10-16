package br.com.oficina.support.saga;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import tools.jackson.databind.JsonNode;

/**
 * Billing Service de mentira para os testes do OS: orcamento e pagamento moram no
 * {@code oficina-billing-service} desde o Passo 3. Atende {@code billing-commands} no proprio processo
 * (fila nao ligada ao SQS nos testes) e responde como {@code docs/contratos} manda - o
 * {@code ContratoDasMensagensDaSagaTest} confere. Tambem faz o papel das pessoas: o cliente decide e
 * paga, o Mercado Pago recusa ou expira.
 *
 * <p>Estado so em memoria e simplificado: o que importa aqui e a Saga do OS, nao as regras do Billing
 * (essas tem os testes do proprio servico).
 */
public class BillingSimulado implements ManipuladorMensagem {

    public static final String SERVICO = "oficina-billing-service";

    public enum StatusOrcamento { AGUARDANDO_APROVACAO, APROVADO, REJEITADO, CANCELADO }

    public enum StatusPagamento { PENDENTE, APROVADO, RECUSADO, EXPIRADO, ESTORNADO, CANCELADO }

    public static final class OrcamentoSimulado {
        private final UUID id = UUID.randomUUID();
        private final String numero = "ORC-" + id;
        private final UUID ordemServicoId;
        private final UUID sagaId;
        private final BigDecimal valorMaoDeObra;
        private final BigDecimal valorPecas;
        private final BigDecimal valorTotal;
        private volatile StatusOrcamento status = StatusOrcamento.AGUARDANDO_APROVACAO;

        OrcamentoSimulado(UUID ordemServicoId, UUID sagaId, BigDecimal valorMaoDeObra, BigDecimal valorPecas,
                          BigDecimal valorTotal) {
            this.ordemServicoId = ordemServicoId;
            this.sagaId = sagaId;
            this.valorMaoDeObra = valorMaoDeObra;
            this.valorPecas = valorPecas;
            this.valorTotal = valorTotal;
        }

        public UUID getId() {
            return id;
        }

        public String getNumeroOrcamento() {
            return numero;
        }

        public UUID getOrdemServicoId() {
            return ordemServicoId;
        }

        public UUID getSagaId() {
            return sagaId;
        }

        public BigDecimal getValorTotal() {
            return valorTotal;
        }

        public StatusOrcamento getStatus() {
            return status;
        }
    }

    public static final class PagamentoSimulado {
        private final UUID id = UUID.randomUUID();
        private final OrcamentoSimulado orcamento;
        private final Instant expiraEm;
        private volatile StatusPagamento status = StatusPagamento.PENDENTE;
        private volatile String referencia;
        private volatile String estorno;

        PagamentoSimulado(OrcamentoSimulado orcamento, Instant expiraEm) {
            this.orcamento = orcamento;
            this.expiraEm = expiraEm;
        }

        public UUID getId() {
            return id;
        }

        public UUID getOrcamentoId() {
            return orcamento.getId();
        }

        public BigDecimal getValor() {
            return orcamento.valorTotal;
        }

        public String getLinkPagamento() {
            return "https://pagamento-simulado.oficina.local/checkout/" + id;
        }

        public Instant getExpiraEm() {
            return expiraEm;
        }

        public StatusPagamento getStatus() {
            return status;
        }

        public String getMpPaymentId() {
            return referencia;
        }

        public String getMpRefundId() {
            return estorno;
        }
    }

    private final PublicadorMensagens publicador;
    private final FabricaEnvelopes fabrica;
    private final TransactionTemplate transacao;
    private final PecaInsumoRepository pecas;
    private final Map<UUID, OrcamentoSimulado> orcamentosPorOrdem = new ConcurrentHashMap<>();
    private final Map<UUID, PagamentoSimulado> pagamentos = new ConcurrentHashMap<>();

    public BillingSimulado(PublicadorMensagens publicador, FabricaEnvelopes fabrica, TransactionTemplate transacao,
                           PecaInsumoRepository pecas) {
        this.publicador = publicador;
        this.fabrica = fabrica.paraServico(SERVICO);
        this.transacao = transacao;
        this.pecas = pecas;
    }

    public void limpar() {
        orcamentosPorOrdem.clear();
        pagamentos.clear();
    }

    // ------------------------------------------------------------------ consultas para os testes

    public Optional<OrcamentoSimulado> orcamentoDaOrdem(UUID ordemServicoId) {
        return Optional.ofNullable(orcamentosPorOrdem.get(ordemServicoId));
    }

    public OrcamentoSimulado orcamento(String numeroOrcamento) {
        return orcamentosPorOrdem.values().stream().filter(o -> o.numero.equals(numeroOrcamento)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Orcamento simulado nao existe: " + numeroOrcamento));
    }

    public Optional<PagamentoSimulado> pagamentoDoOrcamento(String numeroOrcamento) {
        UUID orcamentoId = orcamento(numeroOrcamento).getId();
        return pagamentos.values().stream().filter(p -> p.getOrcamentoId().equals(orcamentoId)).findFirst();
    }

    // ------------------------------------------------------------------ comandos da Saga

    @Override
    public Set<String> tipos() {
        return Set.of(MensagensSaga.GERAR_ORCAMENTO, MensagensSaga.REVERTER_APROVACAO, MensagensSaga.CANCELAR_ORCAMENTO,
                MensagensSaga.SOLICITAR_PAGAMENTO, MensagensSaga.ESTORNAR_PAGAMENTO);
    }

    @Override
    public void tratar(Envelope comando) {
        switch (comando.type()) {
            case MensagensSaga.GERAR_ORCAMENTO -> gerar(comando);
            case MensagensSaga.REVERTER_APROVACAO -> {
                OrcamentoSimulado orcamento = porId(comando.payload().get("orcamentoId").asString());
                orcamento.status = StatusOrcamento.AGUARDANDO_APROVACAO;
                responder(comando, MensagensSaga.APROVACAO_REVERTIDA, Map.of("orcamentoId", orcamento.id.toString()));
            }
            case MensagensSaga.CANCELAR_ORCAMENTO -> {
                OrcamentoSimulado orcamento = porId(comando.payload().get("orcamentoId").asString());
                orcamento.status = StatusOrcamento.CANCELADO;
                boolean cancelouCobranca = false;
                for (PagamentoSimulado pagamento : pagamentos.values()) {
                    if (pagamento.orcamento == orcamento && pagamento.status == StatusPagamento.PENDENTE) {
                        pagamento.status = StatusPagamento.CANCELADO;
                        cancelouCobranca = true;
                    }
                }
                responder(comando, MensagensSaga.ORCAMENTO_CANCELADO,
                        Map.of("orcamentoId", orcamento.id.toString(), "pagamentoCancelado", cancelouCobranca));
            }
            case MensagensSaga.SOLICITAR_PAGAMENTO -> solicitarPagamento(comando);
            case MensagensSaga.ESTORNAR_PAGAMENTO -> estornar(comando);
            default -> throw new IllegalArgumentException(comando.type());
        }
    }

    private void gerar(Envelope comando) {
        OrcamentoSimulado existente = orcamentosPorOrdem.get(comando.ordemServicoId());
        if (existente != null) {
            responderGerado(comando, existente);
            return;
        }
        JsonNode pedido = comando.payload();
        List<String> invalidas = new ArrayList<>();
        BigDecimal valorPecas = BigDecimal.ZERO;
        for (JsonNode item : pedido.path("pecas")) {
            Optional<PecaInsumo> peca = pecas.buscarPorId(item.get("pecaInsumoId").asString());
            if (peca.isEmpty()) {
                invalidas.add(item.get("pecaInsumoId").asString());
            } else {
                valorPecas = valorPecas.add(peca.get().getPreco().multiply(BigDecimal.valueOf(item.get("quantidade").asInt())));
            }
        }
        if (!invalidas.isEmpty()) {
            responder(comando, MensagensSaga.ORCAMENTO_FALHOU, Map.of(
                    "motivo", "Peca/Insumo nao encontrada com o ID: " + String.join(", ", invalidas), "pecasInvalidas", invalidas));
            return;
        }
        BigDecimal maoDeObra = valor(pedido.path("valorMaoDeObra"));
        BigDecimal desconto = valor(pedido.path("desconto"));
        BigDecimal total = maoDeObra.add(valorPecas).subtract(desconto);
        if (total.signum() < 0) {
            responder(comando, MensagensSaga.ORCAMENTO_FALHOU, Map.of("motivo", "Desconto nao pode ser maior que o valor total do orcamento."));
            return;
        }
        OrcamentoSimulado orcamento = new OrcamentoSimulado(comando.ordemServicoId(), comando.sagaId(), maoDeObra, valorPecas, total);
        orcamentosPorOrdem.put(comando.ordemServicoId(), orcamento);
        responderGerado(comando, orcamento);
    }

    /** OrcamentoGerado; o mesmo orcamento para o comando repetido (idempotencia por OS, como o Billing). */
    private void responderGerado(Envelope comando, OrcamentoSimulado orcamento) {
        String validade = comando.payload().hasNonNull("validade")
                ? comando.payload().get("validade").asString()
                : Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        Map<String, Object> gerado = new LinkedHashMap<>();
        gerado.put("orcamentoId", orcamento.id.toString());
        gerado.put("numeroOrcamento", orcamento.numero);
        gerado.put("valorMaoDeObra", dinheiro(orcamento.valorMaoDeObra));
        gerado.put("valorPecas", dinheiro(orcamento.valorPecas));
        gerado.put("desconto", dinheiro(orcamento.valorMaoDeObra.add(orcamento.valorPecas).subtract(orcamento.valorTotal)));
        gerado.put("valorTotal", dinheiro(orcamento.valorTotal));
        gerado.put("validade", validade);
        responder(comando, MensagensSaga.ORCAMENTO_GERADO, gerado);
    }

    private void solicitarPagamento(Envelope comando) {
        OrcamentoSimulado orcamento = porId(comando.payload().get("orcamentoId").asString());
        PagamentoSimulado pagamento = pagamentos.values().stream()
                .filter(p -> p.orcamento == orcamento && (p.status == StatusPagamento.PENDENTE || p.status == StatusPagamento.APROVADO))
                .findFirst()
                .orElseGet(() -> {
                    PagamentoSimulado novo = new PagamentoSimulado(orcamento, Instant.parse(comando.payload().get("expiraEm").asString()));
                    pagamentos.put(novo.id, novo);
                    return novo;
                });
        responder(comando, MensagensSaga.PAGAMENTO_SOLICITADO, Map.of(
                "pagamentoId", pagamento.id.toString(),
                "valor", dinheiro(orcamento.valorTotal),
                "linkPagamento", pagamento.getLinkPagamento(),
                "expiraEm", pagamento.expiraEm.toString()));
    }

    private void estornar(Envelope comando) {
        String pagamentoId = comando.payload().get("pagamentoId").asString();
        PagamentoSimulado pagamento = pagamentos.get(UUID.fromString(pagamentoId));
        if (pagamento == null || (pagamento.status != StatusPagamento.APROVADO && pagamento.status != StatusPagamento.ESTORNADO)) {
            responder(comando, MensagensSaga.ESTORNO_FALHOU, Map.of("pagamentoId", pagamentoId,
                    "motivo", pagamento == null ? "Pagamento nao encontrado" : "Pagamento em " + pagamento.status + " nao pode ser estornado"));
            return;
        }
        if (pagamento.status == StatusPagamento.APROVADO) {
            pagamento.status = StatusPagamento.ESTORNADO;
            pagamento.estorno = "SIM-REFUND-" + pagamento.id.toString().substring(0, 8);
            pagamento.orcamento.status = StatusOrcamento.CANCELADO;
        }
        responder(comando, MensagensSaga.PAGAMENTO_ESTORNADO, Map.of(
                "pagamentoId", pagamento.id.toString(), "valor", dinheiro(pagamento.orcamento.valorTotal), "mpRefundId", pagamento.estorno));
    }

    // ------------------------------------------------------------------ pessoas e provedor

    /** Cliente (ou funcionario, ou integracao) decide. Evento espontaneo, sem causationId. */
    public void decidir(String numeroOrcamento, boolean aprovado, String canal) {
        OrcamentoSimulado orcamento = orcamento(numeroOrcamento);
        if (orcamento.status != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("Orcamento " + numeroOrcamento + " em " + orcamento.status);
        }
        orcamento.status = aprovado ? StatusOrcamento.APROVADO : StatusOrcamento.REJEITADO;
        espontaneo(orcamento, aprovado ? MensagensSaga.ORCAMENTO_APROVADO : MensagensSaga.ORCAMENTO_REJEITADO, Map.of(
                "orcamentoId", orcamento.id.toString(), "canal", canal, "decididoEm", agora()));
    }

    /** Cliente paga no checkout; o Mercado Pago confirma. */
    public void pagar(String numeroOrcamento) {
        PagamentoSimulado pagamento = cobrancaPendente(numeroOrcamento);
        pagamento.status = StatusPagamento.APROVADO;
        pagamento.referencia = "SIM-PAY-" + pagamento.id.toString().substring(0, 8);
        espontaneo(pagamento.orcamento, MensagensSaga.PAGAMENTO_CONFIRMADO, Map.of(
                "pagamentoId", pagamento.id.toString(), "mpPaymentId", pagamento.referencia,
                "valor", dinheiro(pagamento.orcamento.valorTotal), "aprovadoEm", agora()));
    }

    public void recusarPagamento(String numeroOrcamento) {
        PagamentoSimulado pagamento = cobrancaPendente(numeroOrcamento);
        pagamento.status = StatusPagamento.RECUSADO;
        espontaneo(pagamento.orcamento, MensagensSaga.PAGAMENTO_RECUSADO, Map.of(
                "pagamentoId", pagamento.id.toString(), "mpPaymentId", "SIM-PAY-RECUSADO", "motivo", "cc_rejected_other_reason"));
    }

    public void expirarPagamento(String numeroOrcamento) {
        PagamentoSimulado pagamento = cobrancaPendente(numeroOrcamento);
        pagamento.status = StatusPagamento.EXPIRADO;
        espontaneo(pagamento.orcamento, MensagensSaga.PAGAMENTO_EXPIRADO, Map.of(
                "pagamentoId", pagamento.id.toString(), "expiradoEm", agora()));
    }

    private PagamentoSimulado cobrancaPendente(String numeroOrcamento) {
        PagamentoSimulado pagamento = pagamentoDoOrcamento(numeroOrcamento)
                .orElseThrow(() -> new IllegalStateException("Orcamento sem cobranca: " + numeroOrcamento));
        if (pagamento.status != StatusPagamento.PENDENTE) {
            throw new IllegalStateException("Cobranca em " + pagamento.status);
        }
        return pagamento;
    }

    // ------------------------------------------------------------------ apoio

    private OrcamentoSimulado porId(String orcamentoId) {
        UUID id = UUID.fromString(orcamentoId);
        return orcamentosPorOrdem.values().stream().filter(o -> o.id.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Orcamento simulado nao existe: " + orcamentoId));
    }

    private void responder(Envelope comando, String tipo, Object payload) {
        publicador.publicar(Filas.OS_SAGA_REPLIES, fabrica.resposta(comando, tipo, payload));
    }

    private void espontaneo(OrcamentoSimulado orcamento, String tipo, Object payload) {
        transacao.executeWithoutResult(s -> publicador.publicar(Filas.OS_SAGA_REPLIES,
                fabrica.novo(tipo, orcamento.sagaId, orcamento.ordemServicoId, payload)));
    }

    private static String agora() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static BigDecimal valor(JsonNode no) {
        return no == null || no.isMissingNode() || no.isNull() ? BigDecimal.ZERO : new BigDecimal(no.asString());
    }

    private static String dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
