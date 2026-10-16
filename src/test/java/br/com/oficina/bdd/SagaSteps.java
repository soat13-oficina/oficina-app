package br.com.oficina.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/**
 * Passos dos cenarios da Saga. Tudo passa pelas APIs HTTP reais; a mensageria assincrona e
 * "esperada" processando o outbox antes de cada verificacao - o que o agendamento faria em ~1 s.
 */
public class SagaSteps {

    /** Validade do orcamento enviado nos cenarios (FluxoOficinaHttp#enviarParaOrcamento). */
    private static final LocalDateTime DEPOIS_DA_VALIDADE = LocalDateTime.of(2030, 1, 2, 0, 0);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private OrquestradorSagaOrdemServico orquestrador;

    @Autowired
    private SagaOrdemServicoRepository sagas;

    @Autowired
    private SpringDataOrcamentoRepository orcamentos;

    private FluxoOficinaHttp fluxo;
    private String numeroOrdem;
    private String numeroOrcamento;

    @Before
    public void preparar() {
        fluxo = new FluxoOficinaHttp(MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build(), contexto);
        fluxo.limpar();
    }

    @After
    public void limpar() {
        fluxo.limpar();
    }

    @Dado("uma ordem de serviço aberta para o veículo {string}")
    public void umaOrdemAberta(String placa) throws Exception {
        numeroOrdem = fluxo.abrirOrdem(placa);
    }

    @E("o diagnóstico concluído com o serviço {string}")
    public void diagnosticoConcluido(String servico) throws Exception {
        fluxo.diagnosticar(numeroOrdem, servico);
    }

    @E("o orçamento enviado ao cliente")
    public void orcamentoEnviado() throws Exception {
        numeroOrcamento = fluxo.enviarParaOrcamento(numeroOrdem);
    }

    @Quando("o cliente aprova o orçamento")
    public void clienteAprova() throws Exception {
        fluxo.clienteAprova(numeroOrcamento);
    }

    @Quando("o cliente rejeita o orçamento")
    public void clienteRejeita() throws Exception {
        fluxo.clienteRejeita(numeroOrcamento);
    }

    @Quando("o mecânico inicia e conclui a execução")
    public void mecanicoExecuta() throws Exception {
        fluxo.processarMensagens();
        fluxo.acao(numeroOrdem, "/execucao/iniciar");
        fluxo.acao(numeroOrdem, "/servico/concluir");
    }

    @Quando("o prazo de decisão vence sem resposta do cliente")
    public void prazoVence() {
        orquestrador.processarPrazosVencidos(DEPOIS_DA_VALIDADE);
    }

    @Entao("a ordem fica {string}")
    public void ordemFica(String situacao) throws Exception {
        fluxo.processarMensagens();
        assertThat(fluxo.ordem(numeroOrdem).getSituacao().getDescricao()).isEqualTo(situacao);
    }

    @Entao("a ordem fica {string} com motivo {string}")
    public void ordemFicaComMotivo(String situacao, String motivo) throws Exception {
        ordemFica(situacao);
        assertThat(fluxo.ordem(numeroOrdem).getMotivoEncerramento()).hasToString(motivo);
    }

    @E("a saga fica {string}")
    public void sagaFica(String status) throws Exception {
        fluxo.processarMensagens();
        fluxo.saga(numeroOrdem)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }

    @E("a saga termina como {string}")
    public void sagaTermina(String status) throws Exception {
        sagaFica(status);
        assertThat(sagas.buscarPorOrdemDeServico(fluxo.ordem(numeroOrdem).getId()).orElseThrow().getStatus().isFinal())
                .as("estado final").isTrue();
    }

    @E("o orçamento fica {string}")
    public void orcamentoFica(String status) {
        fluxo.processarMensagens();
        assertThat(orcamentos.findAll()).singleElement()
                .satisfies(orcamento -> assertThat(orcamento.getStatus()).hasToString(status));
    }

    @E("a história da saga registra o comando {string} e a confirmação {string}")
    public void historiaRegistra(String comando, String confirmacao) {
        List<RegistroPassoSaga> passos = sagas.listarPassos(
                sagas.buscarPorOrdemDeServico(fluxo.ordem(numeroOrdem).getId()).orElseThrow().getId());
        assertThat(passos).anySatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(RegistroPassoSaga.Tipo.COMANDO_ENVIADO);
            assertThat(p.getDetalhe()).isEqualTo(comando);
        });
        assertThat(passos).anySatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
            assertThat(p.getDetalhe()).startsWith(confirmacao);
        });
    }
}
