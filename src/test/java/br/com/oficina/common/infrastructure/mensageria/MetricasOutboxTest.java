package br.com.oficina.common.infrastructure.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.support.saga.FluxoOficinaHttp;
import io.micrometer.core.instrument.MeterRegistry;

/** Os gauges do outbox acompanham o que realmente esta parado na tabela. */
@SpringBootTest
class MetricasOutboxTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private MeterRegistry registry;

    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void preparar() {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        fluxo = new FluxoOficinaHttp(mvc, contexto);
        fluxo.limpar();
    }

    @AfterEach
    void limpar() {
        fluxo.limpar();
    }

    private double gauge(String nome) {
        return registry.get(nome).gauge().value();
    }

    @Test
    void mensagemNaoEntregueAparecePendenteComIdadeEDesapareceAoSerEntregue() throws Exception {
        assertThat(gauge("oficina.outbox.pendentes")).isZero();
        assertThat(gauge("oficina.outbox.idade.maxima")).isZero();

        fluxo.abrirOrdemSemEntregarMensagens("OUT1A11");

        assertThat(gauge("oficina.outbox.pendentes")).isGreaterThanOrEqualTo(1);
        assertThat(gauge("oficina.outbox.idade.maxima")).isGreaterThanOrEqualTo(0);
        assertThat(gauge("oficina.outbox.esgotadas")).isZero();

        fluxo.processarMensagens();

        assertThat(gauge("oficina.outbox.pendentes")).isZero();
        assertThat(gauge("oficina.outbox.idade.maxima")).isZero();
    }
}
