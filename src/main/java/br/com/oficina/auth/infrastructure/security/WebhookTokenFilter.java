package br.com.oficina.auth.infrastructure.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class WebhookTokenFilter extends OncePerRequestFilter {
    private static final String HEADER_TOKEN = "X-Webhook-Token";
    private static final String PREFIXO_WEBHOOK_ORCAMENTO = "/integracoes/orcamentos/";

    private final String webhookSecret;

    public WebhookTokenFilter(@Value("${orcamento.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!ehWebhookOrcamento(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(HEADER_TOKEN);
        if (token == null || !token.equals(webhookSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean ehWebhookOrcamento(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith(PREFIXO_WEBHOOK_ORCAMENTO)
                && request.getRequestURI().endsWith("/decisao");
    }
}
