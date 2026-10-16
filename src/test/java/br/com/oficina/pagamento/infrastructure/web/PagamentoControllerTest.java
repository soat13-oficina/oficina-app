package br.com.oficina.pagamento.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.pagamento.domain.model.Pagamento;
import br.com.oficina.pagamento.domain.repository.PagamentoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Contrato HTTP da cobranca e da simulacao. O caminho feliz (cobranca criada pela Saga, paga e
 * refletida na OS) esta em OrquestradorSagaOrdemServicoTest e nos cenarios BDD.
 */
@SpringBootTest
class PagamentoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PagamentoRepository pagamentos;

    private MockMvc mvc;
    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        fluxo = new FluxoOficinaHttp(mvc, contexto);
        fluxo.limpar();
    }

    @AfterEach
    void limpar() {
        fluxo.limpar();
    }

    /** Cobranca solta, sem orcamento - so para o contrato HTTP (H2 sem FK para orcamentos no create-drop do teste). */
    private Pagamento cobranca() {
        Pagamento pagamento = Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("530"), LocalDateTime.now().plusHours(24), LocalDateTime.now());
        pagamento.registrarCobranca("SIM-PREF-1", "https://pagamento-simulado.oficina.local/checkout/" + pagamento.getId(), LocalDateTime.now());
        return pagamentos.salvar(pagamento);
    }

    private org.springframework.test.web.servlet.ResultActions simular(UUID pagamentoId, String corpo) throws Exception {
        return mvc.perform(post("/pagamentos/" + pagamentoId + "/simulacao").with(FluxoOficinaHttp.ATENDENTE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void consultaMostraValorStatusLinkEPrazo() throws Exception {
        Pagamento pagamento = cobranca();

        mvc.perform(get("/pagamentos/" + pagamento.getId()).with(FluxoOficinaHttp.ATENDENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamentoId").value(pagamento.getId().toString()))
                .andExpect(jsonPath("$.orcamentoId").value(pagamento.getOrcamentoId().toString()))
                .andExpect(jsonPath("$.valor").value(530))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.linkPagamento").value(pagamento.getLinkPagamento()))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.referenciaPagamento").doesNotExist());
    }

    @Test
    void simulacaoAprovaERepetirOMesmoResultadoNaoEhErro() throws Exception {
        Pagamento pagamento = cobranca();

        simular(pagamento.getId(), "{ \"resultado\": \"APROVADO\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.referenciaPagamento").value(org.hamcrest.Matchers.startsWith("SIM-PAY-")));
        simular(pagamento.getId(), "{ \"resultado\": \"APROVADO\" }").andExpect(status().isOk());
        simular(pagamento.getId(), "{ \"resultado\": \"RECUSADO\", \"motivo\": \"tarde demais\" }")
                .andExpect(status().isConflict());
    }

    @Test
    void simulacaoSemResultadoOuComResultadoInvalidoResponde400() throws Exception {
        Pagamento pagamento = cobranca();

        simular(pagamento.getId(), "{ }").andExpect(status().isBadRequest());
        simular(pagamento.getId(), "{ \"resultado\": \"TALVEZ\" }").andExpect(status().isBadRequest());
    }

    @Test
    void pagamentoOuOrcamentoInexistenteResponde404() throws Exception {
        mvc.perform(get("/pagamentos/" + UUID.randomUUID()).with(FluxoOficinaHttp.ATENDENTE))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orcamentos/ORC-NAO-EXISTE/pagamento").with(FluxoOficinaHttp.ATENDENTE))
                .andExpect(status().isNotFound());
        simular(UUID.randomUUID(), "{ \"resultado\": \"APROVADO\" }").andExpect(status().isNotFound());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/pagamentos/" + UUID.randomUUID())).andExpect(status().isForbidden());
    }
}
