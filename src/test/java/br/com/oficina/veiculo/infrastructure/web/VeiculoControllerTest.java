package br.com.oficina.veiculo.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class VeiculoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCadastrarVeiculo() throws Exception {
        String requestBody = """
                {
                  "placa": "ABC1D23",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "fabricante": "Toyota Motor Corporation",
                  "ano": 2024,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "cliente-1"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/veiculos/ABC1D23"));
    }

    @Test
    void deveListarVeiculosFiltrandoPorMarcaETipo() throws Exception {
        String corolla = """
                {
                  "placa": "ABC1D23",
                  "marca": "MarcaListaUnica",
                  "modelo": "Corolla",
                  "fabricante": "Fabricante Lista Unica",
                  "ano": 2024,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "cliente-1"
                }
                """;
        String tesla = """
                {
                  "placa": "TES1A23",
                  "marca": "Tesla",
                  "modelo": "Model 3",
                  "fabricante": "Tesla Inc.",
                  "ano": 2023,
                  "potencia": 283,
                  "cambio": "AUTOMATICO",
                  "tipo": "ELETRICO",
                  "clienteId": "cliente-2"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corolla))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tesla))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "MarcaListaUnica")
                        .param("tipo", "FLEX")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"))
                .andExpect(jsonPath("$[0].fabricante").value("Fabricante Lista Unica"))
                .andExpect(jsonPath("$[0].ano").value(2024))
                .andExpect(jsonPath("$[0].potencia").value(177))
                .andExpect(jsonPath("$[0].cambio").value("AUTOMATICO"))
                .andExpect(jsonPath("$[0].tipo").value("FLEX"));
    }

    @Test
    void deveExcluirVeiculo() throws Exception {
        String requestBody = """
                {
                  "placa": "DEL1E23",
                  "marca": "Ford",
                  "modelo": "Ranger",
                  "fabricante": "Ford Motor Company",
                  "ano": 2022,
                  "potencia": 213,
                  "cambio": "AUTOMATICO",
                  "tipo": "DIESEL",
                  "clienteId": "cliente-3"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/veiculos/DEL1E23")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "Ford")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveAlterarVeiculo() throws Exception {
        String cadastro = """
                {
                  "placa": "ALT1E23",
                  "marca": "Volkswagen",
                  "modelo": "T-Cross",
                  "fabricante": "Volkswagen AG",
                  "ano": 2023,
                  "potencia": 128,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "cliente-4"
                }
                """;
        String alteracao = """
                {
                  "marca": "Volkswagen",
                  "modelo": "Taos",
                  "fabricante": "Volkswagen AG",
                  "ano": 2024,
                  "potencia": 150,
                  "cambio": "AUTOMATICO",
                  "tipo": "GASOLINA"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/veiculos/ALT1E23")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "Volkswagen")
                        .param("ano", "2024")
                        .param("tipo", "GASOLINA")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ALT1E23"))
                .andExpect(jsonPath("$[0].modelo").value("Taos"))
                .andExpect(jsonPath("$[0].potencia").value(150))
                .andExpect(jsonPath("$[0].tipo").value("GASOLINA"));
    }
}
