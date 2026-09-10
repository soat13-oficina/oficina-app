package br.com.oficina.common.infrastructure.observabilidade;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ObservabilidadeWebConfig implements WebMvcConfigurer {

    private final OrdemDeServicoMdcInterceptor ordemDeServicoMdcInterceptor;

    public ObservabilidadeWebConfig(OrdemDeServicoMdcInterceptor ordemDeServicoMdcInterceptor) {
        this.ordemDeServicoMdcInterceptor = ordemDeServicoMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ordemDeServicoMdcInterceptor);
    }
}
