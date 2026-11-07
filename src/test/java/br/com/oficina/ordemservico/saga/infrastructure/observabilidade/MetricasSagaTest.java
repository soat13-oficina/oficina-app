package br.com.oficina.ordemservico.saga.infrastructure.observabilidade;

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

/**
 * As metricas da Saga saem das transicoes reais (HTTP, outbox, orquestrador, participantes simulados), e
 * nao de chamadas diretas ao componente: o que se confere e o que o Datadog vai ver.
 */
@SpringBootTest
class MetricasSagaTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private MetricasSaga metricas;

    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void preparar() {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        fluxo = new FluxoOficinaHttp(mvc, contexto);
        fluxo.limpar();
        metricas.invalidarCache();
    }

    @AfterEach
    void limpar() {
        fluxo.limpar();
    }

    private double contador(String nome, String... tags) {
        var c = registry.find(nome).tags(tags).counter();
        return c == null ? 0 : c.count();
    }

    private double gauge(String nome, String... tags) {
        return registry.get(nome).tags(tags).gauge().value();
    }

    private long amostras(String resultado) {
        var t = registry.find("oficina.saga.duracao").tag("resultado", resultado).timer();
        return t == null ? 0 : t.count();
    }

    @Test
    void sagaConcluidaContaIniciadaEFinalizadaECronometraADuracao() throws Exception {
        double iniciadas = contador("oficina.saga.iniciadas");
        double concluidas = contador("oficina.saga.finalizadas", "resultado", "CONCLUIDA");
        long duracoes = amostras("CONCLUIDA");

        String numero = fluxo.abrirOrdem("MET1A11");
        assertThat(contador("oficina.saga.iniciadas")).isEqualTo(iniciadas + 1);
        assertThat(contador("oficina.saga.finalizadas", "resultado", "CONCLUIDA")).isEqualTo(concluidas);

        fluxo.diagnosticar(numero, "Troca de oleo");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();
        fluxo.simularPagamento(orcamento, "APROVADO");
        fluxo.processarMensagens();
        fluxo.iniciarExecucao(numero);
        fluxo.concluirExecucao(numero);

        assertThat(contador("oficina.saga.iniciadas")).isEqualTo(iniciadas + 1);
        assertThat(contador("oficina.saga.finalizadas", "resultado", "CONCLUIDA")).isEqualTo(concluidas + 1);
        assertThat(amostras("CONCLUIDA")).isEqualTo(duracoes + 1);
    }

    @Test
    void compensacaoPorExecucaoAbortadaContaComOMotivoDoEncerramento() throws Exception {
        double compensacoes = contador("oficina.saga.compensacoes", "motivo", "FALHA_EXECUCAO");
        double compensadas = contador("oficina.saga.finalizadas", "resultado", "COMPENSADA");

        String numero = fluxo.abrirOrdem("MET2B22");
        fluxo.diagnosticar(numero, "Troca de oleo");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();
        fluxo.simularPagamento(orcamento, "APROVADO");
        fluxo.processarMensagens();
        fluxo.iniciarExecucao(numero);
        fluxo.abortarExecucao(numero, "Peca errada");

        assertThat(contador("oficina.saga.compensacoes", "motivo", "FALHA_EXECUCAO")).isEqualTo(compensacoes + 1);
        assertThat(contador("oficina.saga.finalizadas", "resultado", "COMPENSADA")).isEqualTo(compensadas + 1);
    }

    @Test
    void funilMostraAsSagasNaoFinalizadasPorPasso() throws Exception {
        assertThat(gauge("oficina.saga.ativas", "passo", "DIAGNOSTICO")).isZero();

        fluxo.abrirOrdem("MET3C33");
        fluxo.abrirOrdem("MET4D44");
        metricas.invalidarCache();

        assertThat(gauge("oficina.saga.ativas", "passo", "DIAGNOSTICO")).isEqualTo(2);
        assertThat(gauge("oficina.saga.ativas", "passo", "PAGAMENTO")).isZero();
        assertThat(gauge("oficina.saga.presas")).as("nenhuma com prazo vencido").isZero();
    }

    @Test
    void motivoComTextoLivreNaoViraTag() {
        assertThat(MetricasSaga.motivo("PAGAMENTO_RECUSADO")).isEqualTo("PAGAMENTO_RECUSADO");
        assertThat(MetricasSaga.motivo("Estoque insuficiente: 8293a4b5 (solicitada 1, disponivel 0)")).isEqualTo("OUTRO");
        assertThat(MetricasSaga.motivo(null)).isEqualTo("OUTRO");
    }
}
