package br.com.oficina.pecainsumo.infrastructure.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.auth.infrastructure.security.ServiceTokenFilter;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;

/** Consulta interna de pecas que o Billing faz ao gerar o orcamento (docs/contratos/rest-interno.md). */
@SpringBootTest
class ConsultaInternaPecasControllerTest {

    private static final String TOKEN = "token-de-servico-de-teste";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PecaInsumoRepository pecas;

    @Autowired
    private SpringDataPecaInsumoRepository springDataPecas;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        springDataPecas.deleteAll();
        pecas.salvar(new PecaInsumo("filtro", "Filtro de oleo", "Bosch", new BigDecimal("80"), 10, 2, "REF-1", CategoriaPeca.FILTROS));
        pecas.salvar(new PecaInsumo("oleo", "Oleo 5W30", "Mobil", new BigDecimal("45.9"), 10, 0, "REF-2", CategoriaPeca.FILTROS));
    }

    @AfterEach
    void limpar() {
        springDataPecas.deleteAll();
    }

    private ResultActions consultar(String token, String corpo) throws Exception {
        var requisicao = post("/internal/pecas-insumos/consulta").contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (token != null) {
            requisicao.header(ServiceTokenFilter.HEADER, token);
        }
        return mvc.perform(requisicao);
    }

    @Test
    void devolvePrecoEDescricaoNaOrdemPedidaEOsIdsQueNaoExistem() throws Exception {
        consultar(TOKEN, "{ \"pecaInsumoIds\": [\"oleo\", \"sumiu\", \"filtro\", \"oleo\"] }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pecas.length()").value(2))
                .andExpect(jsonPath("$.pecas[0].pecaInsumoId").value("oleo"))
                .andExpect(jsonPath("$.pecas[0].descricao").value("Oleo 5W30"))
                .andExpect(jsonPath("$.pecas[0].preco").value("45.90"))
                .andExpect(jsonPath("$.pecas[1].pecaInsumoId").value("filtro"))
                .andExpect(jsonPath("$.pecas[1].preco").value("80.00"))
                .andExpect(jsonPath("$.inexistentes[0]").value("sumiu"))
                .andExpect(jsonPath("$.inexistentes.length()").value(1));
    }

    @Test
    void semTokenOuComTokenErradoResponde401() throws Exception {
        consultar(null, "{ \"pecaInsumoIds\": [\"filtro\"] }").andExpect(status().isUnauthorized());
        consultar("outro", "{ \"pecaInsumoIds\": [\"filtro\"] }").andExpect(status().isUnauthorized());
    }

    @Test
    void listaVaziaOuGrandeDemaisResponde400() throws Exception {
        consultar(TOKEN, "{ \"pecaInsumoIds\": [] }").andExpect(status().isBadRequest());
        consultar(TOKEN, "{ }").andExpect(status().isBadRequest());
        String muitos = IntStream.rangeClosed(1, 201).mapToObj(i -> "\"p" + i + "\"").collect(Collectors.joining(","));
        consultar(TOKEN, "{ \"pecaInsumoIds\": [" + muitos + "] }").andExpect(status().isBadRequest());
    }

    @Test
    void idEmBrancoContaComoInexistente() throws Exception {
        consultar(TOKEN, "{ \"pecaInsumoIds\": [\" \", \"filtro\"] }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pecas[0].pecaInsumoId").value("filtro"))
                .andExpect(jsonPath("$.inexistentes[0]").value(" "));
    }

    @Test
    void semTokenConfiguradoNenhumaChamadaInternaEhAceita() throws Exception {
        ServiceTokenFilter semToken = new ServiceTokenFilter("");
        var requisicao = new org.springframework.mock.web.MockHttpServletRequest("POST", "/internal/pecas-insumos/consulta");
        requisicao.addHeader(ServiceTokenFilter.HEADER, "");
        var resposta = new org.springframework.mock.web.MockHttpServletResponse();

        semToken.doFilter(requisicao, resposta, new org.springframework.mock.web.MockFilterChain());

        org.assertj.core.api.Assertions.assertThat(resposta.getStatus()).isEqualTo(401);
    }
}
