package br.com.oficina.pecainsumo.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PecaInsumoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String cadastrarPeca(String descricao, String marca, String preco,
                                  int quantidadeEstoque, String codigoReferencia, String categoria) throws Exception {
        String requestBody = """
                {
                  "descricao": "%s",
                  "marca": "%s",
                  "preco": %s,
                  "quantidadeEstoque": %d,
                  "codigoReferencia": "%s",
                  "categoria": "%s"
                }
                """.formatted(descricao, marca, preco, quantidadeEstoque, codigoReferencia, categoria);

        MvcResult result = mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getHeader("Location");
    }

    @Test
    void deveCadastrarPecaInsumo() throws Exception {
        String requestBody = """
                {
                  "descricao": "Filtro de óleo motor",
                  "marca": "Bosch",
                  "preco": 45.90,
                  "quantidadeEstoque": 10,
                  "codigoReferencia": "OB0986B01044",
                  "categoria": "FILTROS"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    void deveCadastrarEConsultarPecaInsumoPorId() throws Exception {
        String requestBody = """
                {
                  "descricao": "Pastilha de freio dianteira",
                  "marca": "TRW",
                  "preco": 120.00,
                  "quantidadeEstoque": 20,
                  "codigoReferencia": "TRW001CTRL",
                  "categoria": "FREIOS"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "TRW")
                        .param("categoria", "FREIOS")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].descricao").value("Pastilha de freio dianteira"))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].marca").value("TRW"))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].preco").value(120.00))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].quantidadeEstoque").value(20))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].quantidadeReservada").value(0))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].quantidadeDisponivel").value(20))
                .andExpect(jsonPath("$[?(@.codigoReferencia=='TRW001CTRL')].categoria").value("FREIOS"));
    }

    @Test
    void deveRetornarErroQuandoPecaInsumoNaoExiste() {
        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(get("/pecas-insumos/id-inexistente-99999")
                        .with(user("tester"))));

        assertInstanceOf(jakarta.persistence.EntityNotFoundException.class, exception.getCause());
    }

    @Test
    void deveListarTodasAsPecasInsumos() throws Exception {
        cadastrarPeca("Filtro de ar CTRL", "Bosch", "35.00", 15, "CTRL_LIST_001", "FILTROS");
        cadastrarPeca("Óleo lubrificante CTRL", "Mobil", "30.00", 50, "CTRL_LIST_002", "LUBRIFICANTES");

        mockMvc.perform(get("/pecas-insumos")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void deveListarPecasInsumosFiltrandoPorMarcaECategoria() throws Exception {
        cadastrarPeca("Filtro combustível CTRL", "MarcaFiltroCtrl", "25.00", 30, "CTRL_FILT_001", "FILTROS");
        cadastrarPeca("Pastilha CTRL", "MarcaFiltroCtrl", "100.00", 10, "CTRL_FILT_002", "FREIOS");

        mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "MarcaFiltroCtrl")
                        .param("categoria", "FILTROS")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigoReferencia").value("CTRL_FILT_001"));
    }

    @Test
    void deveAlterarPecaInsumo() throws Exception {
        cadastrarPeca("Vela ignição original CTRL", "NGK", "28.50", 15, "CTRL_ALT_001", "IGNICAO");

        String idPeca = null;
        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "NGK")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_ALT_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        String alteracaoBody = """
                {
                  "descricao": "Vela ignição iridium CTRL",
                  "marca": "NGK",
                  "preco": 55.00,
                  "quantidadeEstoque": 20,
                  "quantidadeReservada": 0,
                  "codigoReferencia": "CTRL_ALT_002",
                  "categoria": "IGNICAO"
                }
                """;

        mockMvc.perform(put("/pecas-insumos/" + idPeca)
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracaoBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Vela ignição iridium CTRL"))
                .andExpect(jsonPath("$.preco").value(55.00))
                .andExpect(jsonPath("$.codigoReferencia").value("CTRL_ALT_002"));
    }

    @Test
    void deveExcluirPecaInsumo() throws Exception {
        cadastrarPeca("Peca para excluir CTRL", "MarcaExcluirCtrl", "10.00", 1, "CTRL_DEL_001", "ESCAPAMENTO");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "MarcaExcluirCtrl")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = nodes.get(0).get("id").asText();

        mockMvc.perform(delete("/pecas-insumos/" + idPeca)
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Verifica que a peça foi excluída usando o endpoint de listagem
        mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "MarcaExcluirCtrl")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveAdicionarEstoque() throws Exception {
        cadastrarPeca("Amortecedor CTRL", "Monroe", "250.00", 4, "CTRL_ADD_001", "SUSPENSAO");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Monroe")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_ADD_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        String addEstoqueBody = """
                {
                  "quantidade": 6
                }
                """;

        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/adicionar-estoque")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addEstoqueBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(10));
    }

    @Test
    void deveRemoverEstoque() throws Exception {
        cadastrarPeca("Correia dentada CTRL", "Gates", "85.00", 12, "CTRL_REM_001", "TRANSMISSAO");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Gates")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_REM_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        String remEstoqueBody = """
                {
                  "quantidade": 5
                }
                """;

        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/remover-estoque")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(remEstoqueBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(7));
    }

    @Test
    void deveReservarPeca() throws Exception {
        cadastrarPeca("Bobina ignição CTRL", "Delphi", "180.00", 8, "CTRL_RSV_001", "ELETRICA");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Delphi")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_RSV_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        String reservarBody = """
                {
                  "quantidade": 3
                }
                """;

        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/reservar")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservarBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeReservada").value(3))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(5));
    }

    @Test
    void deveLiberarReservaDePeca() throws Exception {
        cadastrarPeca("Bomba combustível CTRL", "Marelli", "320.00", 6, "CTRL_LIB_001", "COMBUSTIVEL");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Marelli")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_LIB_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        // Primeiro reservar
        String reservarBody = """
                {
                  "quantidade": 4
                }
                """;

        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/reservar")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservarBody))
                .andExpect(status().isNoContent());

        // Depois liberar parcialmente
        String liberarBody = """
                {
                  "quantidade": 2
                }
                """;

        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/liberar-reserva")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(liberarBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeReservada").value(2))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(4));
    }

    @Test
    void deveRetornarBadRequestQuandoPayloadForInvalido() throws Exception {
        String requestBody = """
                {
                  "descricao": "Filtro de óleo",
                  "marca": "Bosch",
                  "preco": 45.90,
                  "quantidadeEstoque": 10,
                  "codigoReferencia": "OB001",
                  "categoria": "CATEGORIA_INVALIDA"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarBadRequestQuandoDescricaoEstiverVazia() throws Exception {
        String requestBody = """
                {
                  "descricao": "",
                  "marca": "Bosch",
                  "preco": 45.90,
                  "quantidadeEstoque": 10,
                  "codigoReferencia": "OB001",
                  "categoria": "FILTROS"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarBadRequestQuandoPrecoForZero() throws Exception {
        String requestBody = """
                {
                  "descricao": "Filtro de óleo",
                  "marca": "Bosch",
                  "preco": 0,
                  "quantidadeEstoque": 10,
                  "codigoReferencia": "OB001",
                  "categoria": "FILTROS"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarBadRequestQuandoCategoriaNaoForInformada() throws Exception {
        String requestBody = """
                {
                  "descricao": "Filtro de óleo",
                  "marca": "Bosch",
                  "preco": 45.90,
                  "quantidadeEstoque": 10,
                  "codigoReferencia": "OB001"
                }
                """;

        mockMvc.perform(post("/pecas-insumos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarPecasComReservaViaFiltro() throws Exception {
        cadastrarPeca("Disco freio CTRL", "Fremax", "150.00", 10, "CTRL_RSVF_001", "FREIOS");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Fremax")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_RSVF_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        // Reservar para criar peça com reserva
        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/reservar")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 2}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pecas-insumos")
                        .param("possuiReserva", "true")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void deveConsumirPecaReservada() throws Exception {
        cadastrarPeca("Sensor temperatura CTRL", "Bosch", "95.00", 10, "CTRL_CONS_001", "ELETRICA");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Bosch")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_CONS_001".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        // Primeiro reservar 4 peças
        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/reservar")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 4}"))
                .andExpect(status().isNoContent());

        // Consumir 2 peças reservadas
        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/consumir")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 2}"))
                .andExpect(status().isNoContent());

        // Verificar que estoque e reserva foram decrementados
        mockMvc.perform(get("/pecas-insumos/" + idPeca)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(8))
                .andExpect(jsonPath("$.quantidadeReservada").value(2))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(6));
    }

    @Test
    void deveFalharAoConsumirMaisDoQueReservado() throws Exception {
        cadastrarPeca("Válvula EGR CTRL", "Delphi", "280.00", 8, "CTRL_CONS_002", "ELETRICA");

        MvcResult listResult = mockMvc.perform(get("/pecas-insumos")
                        .param("marca", "Delphi")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nodes = mapper.readTree(responseBody);
        String idPeca = null;
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            if ("CTRL_CONS_002".equals(node.get("codigoReferencia").asText())) {
                idPeca = node.get("id").asText();
                break;
            }
        }

        // Reservar 2 peças
        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/reservar")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 2}"))
                .andExpect(status().isNoContent());

        // Tentar consumir 5 (mais do que reservado) - deve falhar
        mockMvc.perform(post("/pecas-insumos/" + idPeca + "/consumir")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 5}"))
                .andExpect(status().isBadRequest());
    }
}
