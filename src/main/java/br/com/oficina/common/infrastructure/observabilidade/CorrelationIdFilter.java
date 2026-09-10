package br.com.oficina.common.infrastructure.observabilidade;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Da a cada requisicao um identificador estavel e o publica no MDC, para que toda linha de log
 * gerada durante o atendimento carregue o mesmo {@code correlation_id} no JSON estruturado.
 *
 * <p>Complementa, e nao substitui, o {@code dd.trace_id} injetado pelo dd-java-agent: o trace id
 * so existe quando o agent esta ativo (nuvem), enquanto o correlation id funciona tambem em
 * desenvolvimento local e sobrevive a chamadas que atravessam o API Gateway, que ja propaga o
 * seu proprio identificador no cabecalho.
 *
 * <p>Roda antes da cadeia do Spring Security ({@link Ordered#HIGHEST_PRECEDENCE}) para que ate
 * as falhas de autenticacao sejam correlacionaveis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_CORRELATION_ID = "correlation_id";
    static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolver(request);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Thread de container e reaproveitada entre requisicoes: sem o remove, a proxima
            // requisicao atendida por esta thread herdaria o id da anterior.
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private String resolver(HttpServletRequest request) {
        String recebido = request.getHeader(HEADER_CORRELATION_ID);
        if (!StringUtils.hasText(recebido)) {
            recebido = request.getHeader(HEADER_REQUEST_ID);
        }
        return StringUtils.hasText(recebido) ? recebido : UUID.randomUUID().toString();
    }
}
