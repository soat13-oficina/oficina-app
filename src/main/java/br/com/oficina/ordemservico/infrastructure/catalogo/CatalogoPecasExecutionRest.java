package br.com.oficina.ordemservico.infrastructure.catalogo;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import br.com.oficina.ordemservico.application.port.CatalogoPecas;

/**
 * O cadastro de pecas e da Execution (ADR 0003): a abertura da OS confere as pecas previstas por REST
 * interno ({@code POST /internal/pecas-insumos/consulta}, contrato em {@code docs/contratos/rest-interno.md}),
 * com o {@code X-Service-Token}. Fora do gateway, pelo DNS do cluster.
 *
 * <p>Timeout curto e uma nova tentativa so em falha de rede (a consulta e idempotente). Erro que sobra
 * sobe: a abertura da OS falha e o atendente tenta de novo - nao se abre ordem com peca nao conferida.
 */
@Component
public class CatalogoPecasExecutionRest implements CatalogoPecas {
    private static final Logger log = LoggerFactory.getLogger(CatalogoPecasExecutionRest.class);
    static final String HEADER_TOKEN = "X-Service-Token";
    static final String HEADER_REQUEST_ID = "x-request-id";
    private static final String MDC_CORRELATION_ID = "correlation_id";

    private final RestClient http;

    public CatalogoPecasExecutionRest(
            @Value("${catalogo-pecas.url}") String url,
            @Value("${seguranca.service-token:}") String serviceToken,
            @Value("${catalogo-pecas.timeout:PT3S}") Duration timeout) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        this.http = RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader(HEADER_TOKEN, serviceToken)
                .build();
    }

    record Requisicao(List<String> pecaInsumoIds) {
    }

    record Resposta(List<String> inexistentes) {
    }

    @Override
    public Set<String> inexistentes(Collection<String> pecaInsumoIds) {
        if (pecaInsumoIds == null || pecaInsumoIds.isEmpty()) {
            return Set.of();
        }
        Requisicao requisicao = new Requisicao(List.copyOf(new LinkedHashSet<>(pecaInsumoIds)));
        Resposta resposta;
        try {
            resposta = chamar(requisicao);
        } catch (ResourceAccessException e) {
            log.warn("Consulta de pecas falhou por rede; nova tentativa. erro={}", e.getMessage());
            resposta = chamar(requisicao);
        }
        if (resposta == null || resposta.inexistentes() == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(resposta.inexistentes());
    }

    private Resposta chamar(Requisicao requisicao) {
        String correlacao = MDC.get(MDC_CORRELATION_ID);
        return http.post()
                .uri("/internal/pecas-insumos/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    if (correlacao != null) {
                        h.set(HEADER_REQUEST_ID, correlacao);
                    }
                })
                .body(requisicao)
                .retrieve()
                .body(Resposta.class);
    }
}
