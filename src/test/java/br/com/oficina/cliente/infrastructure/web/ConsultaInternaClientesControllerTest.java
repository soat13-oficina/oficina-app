package br.com.oficina.cliente.infrastructure.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.auth.infrastructure.security.ServiceTokenFilter;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/** Consulta de cliente por CPF que a Lambda de autenticacao faz (docs/contratos/rest-interno.md). */
@SpringBootTest
class ConsultaInternaClientesControllerTest {

    private static final String TOKEN = "token-de-servico-de-teste";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private SpringDataClienteRepository springDataClientes;

    @Autowired
    private SpringDataVeiculoRepository veiculos;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        limpar();
    }

    @AfterEach
    void limpar() {
        veiculos.deleteAll();
        springDataClientes.deleteAll();
    }

    private ResultActions consultar(String token, String cpf) throws Exception {
        var requisicao = get("/internal/clientes/por-cpf/{cpf}", cpf);
        if (token != null) {
            requisicao = requisicao.header(ServiceTokenFilter.HEADER, token);
        }
        return mvc.perform(requisicao);
    }

    @Test
    void devolveIdNomeEAtivoDoClienteComCpfSemMascara() throws Exception {
        Cliente maria = clientes.salvar(new Cliente("Maria da Silva", "52998224725", TipoCliente.PF));

        consultar(TOKEN, "52998224725")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maria.getId().toString()))
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void encontraOClienteGravadoComMascaraEConsultadoSemOuComEla() throws Exception {
        // O cadastro guarda o documento como foi digitado: a comparacao e so pelos digitos.
        Cliente maria = clientes.salvar(new Cliente("Maria da Silva", "529.982.247-25", TipoCliente.PF));

        consultar(TOKEN, "52998224725").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(maria.getId().toString()));
        consultar(TOKEN, "529.982.247-25").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(maria.getId().toString()));
    }

    @Test
    void clienteInativoVoltaComAtivoFalso() throws Exception {
        Cliente joao = clientes.salvar(new Cliente("Joao", "12345678909", TipoCliente.PF));
        jdbc.update("UPDATE clientes SET ativo = FALSE WHERE id = ?", joao.getId());

        consultar(TOKEN, "12345678909")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void cpfSemCadastroResponde404() throws Exception {
        consultar(TOKEN, "52998224725").andExpect(status().isNotFound());
    }

    @Test
    void cpfSemOnzeDigitosResponde400() throws Exception {
        consultar(TOKEN, "123").andExpect(status().isBadRequest());
        // Um CNPJ nao e CPF: nunca devolve cliente PJ por esta rota.
        consultar(TOKEN, "11222333000181").andExpect(status().isBadRequest());
    }

    @Test
    void semTokenDeServicoOuComTokenErradoResponde401() throws Exception {
        clientes.salvar(new Cliente("Maria da Silva", "52998224725", TipoCliente.PF));

        consultar(null, "52998224725").andExpect(status().isUnauthorized());
        consultar("outro-token", "52998224725").andExpect(status().isUnauthorized());
    }
}
