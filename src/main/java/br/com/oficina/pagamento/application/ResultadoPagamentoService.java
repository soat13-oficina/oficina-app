package br.com.oficina.pagamento.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.pagamento.application.saga.EventosPagamento;
import br.com.oficina.pagamento.domain.model.Pagamento;
import br.com.oficina.pagamento.domain.repository.PagamentoRepository;

/**
 * Resultado de uma cobranca: aprovacao/recusa (hoje pela simulacao; no Passo 3 pelo webhook do Mercado
 * Pago, depois de consultar o pagamento na API) e expiracao pelo prazo. Cada mudanca publica o evento
 * para a Saga - o Billing e a autoridade do pagamento (ADR 0004, timeouts).
 */
@Service
public class ResultadoPagamentoService {
    private static final Logger log = LoggerFactory.getLogger(ResultadoPagamentoService.class);

    public enum Resultado {
        APROVADO,
        RECUSADO
    }

    private final PagamentoRepository pagamentos;
    private final OrcamentoRepository orcamentos;
    private final EventosPagamento eventos;

    public ResultadoPagamentoService(PagamentoRepository pagamentos, OrcamentoRepository orcamentos, EventosPagamento eventos) {
        this.pagamentos = pagamentos;
        this.orcamentos = orcamentos;
        this.eventos = eventos;
    }

    /**
     * Registra o resultado. Repetir o mesmo resultado nao e erro (o webhook chega mais de uma vez) e
     * nao publica de novo; resultado diferente de um ja registrado e conflito (409).
     */
    @Transactional
    public Pagamento registrar(UUID pagamentoId, Resultado resultado, String referenciaPagamento, String motivo) {
        if (resultado == null) {
            throw new RegraDeNegocioException("Resultado do pagamento e obrigatorio (APROVADO ou RECUSADO).");
        }
        Pagamento pagamento = consultar(pagamentoId);
        LocalDateTime agora = LocalDateTime.now();
        if (resultado == Resultado.APROVADO) {
            if (pagamento.aprovar(referenciaPagamento, agora)) {
                pagamentos.salvar(pagamento);
                eventos.confirmado(pagamento);
                log.info("Pagamento aprovado. pagamentoId={}, valor={}", pagamento.getId(), pagamento.getValor());
            }
        } else if (pagamento.recusar(referenciaPagamento, motivo == null || motivo.isBlank() ? "Pagamento recusado" : motivo, agora)) {
            pagamentos.salvar(pagamento);
            eventos.recusado(pagamento);
            log.info("Pagamento recusado. pagamentoId={}, motivo={}", pagamento.getId(), pagamento.getMotivo());
        }
        return pagamento;
    }

    /** Expira as cobrancas pendentes com prazo vencido; devolve quantas expirou. */
    @Transactional
    public int expirarVencidos(LocalDateTime agora) {
        List<Pagamento> vencidos = pagamentos.buscarPendentesVencidos(agora);
        for (Pagamento pagamento : vencidos) {
            pagamento.expirar(agora);
            pagamentos.salvar(pagamento);
            eventos.expirado(pagamento);
            log.info("Pagamento expirado. pagamentoId={}, expiraEm={}", pagamento.getId(), pagamento.getExpiraEm());
        }
        return vencidos.size();
    }

    @Transactional(readOnly = true)
    public Pagamento consultar(UUID pagamentoId) {
        return pagamentos.buscarPorId(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento nao encontrado: " + pagamentoId));
    }

    /** Cobranca mais recente do orcamento. */
    @Transactional(readOnly = true)
    public Pagamento consultarPorOrcamento(String numeroOrcamento) {
        UUID orcamentoId = orcamentos.buscarPorNumeroOrcamento(numeroOrcamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."))
                .getId();
        return pagamentos.buscarPorOrcamento(orcamentoId).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento ainda sem cobranca: o pagamento e pedido depois da aprovacao e da reserva das pecas."));
    }
}
