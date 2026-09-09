package br.com.oficina.common.infrastructure.observabilidade;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Publica o numero da OS no MDC quando a rota atendida tem {@code {numeroOrdemServico}} no path.
 *
 * <p>E o que permite pivotar a investigacao pela chave que o negocio usa: com o log em JSON,
 * {@code numero_ordem_servico:OS-1A2B3C4D} devolve o rastro inteiro daquela ordem - abertura,
 * diagnostico, orcamento, entrega - em vez de exigir que se descubra antes qual foi o trace id.
 *
 * <p>Um interceptor, e nao codigo espalhado pelos controllers: o Spring ja resolveu as variaveis
 * de path em {@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE} antes de chamar o handler,
 * entao todos os endpoints de OS ganham o atributo sem nenhuma mudanca nos casos de uso.
 */
@Component
public class OrdemDeServicoMdcInterceptor implements HandlerInterceptor {

    public static final String MDC_NUMERO_ORDEM_SERVICO = "numero_ordem_servico";
    private static final String VARIAVEL_PATH = "numeroOrdemServico";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object variaveis = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variaveis instanceof Map<?, ?> mapa) {
            Object numero = mapa.get(VARIAVEL_PATH);
            if (numero != null) {
                MDC.put(MDC_NUMERO_ORDEM_SERVICO, numero.toString());
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_NUMERO_ORDEM_SERVICO);
    }
}
