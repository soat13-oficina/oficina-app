package br.com.oficina.ordemservico.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

/**
 * Contrato do historico de status (migration V21): toda mudanca de STATUS vira uma linha, com a
 * origem da mudanca - inclusive as que ficam dentro da mesma situacao de negocio.
 */
class OrdemDeServicoHistoricoTest {

    private static final UUID CLIENTE_ID = UUID.fromString("91111111-1111-1111-1111-111111111111");

    @Test
    void abrirRegistraAEntradaEmOsAbertaSemStatusAnterior() {
        OrdemDeServico ordem = abrir();

        List<AlteracaoStatusOrdemDeServico> alteracoes = ordem.descarregarAlteracoesDeStatus();

        assertThat(alteracoes).singleElement().satisfies(alteracao -> {
            assertThat(alteracao.statusAnterior()).isNull();
            assertThat(alteracao.statusNovo()).isEqualTo(StatusOrdemDeServico.OS_ABERTA);
            assertThat(alteracao.situacaoNova()).isEqualTo(SituacaoOrdemDeServico.RECEBIDA);
            assertThat(alteracao.origem()).isEqualTo(OrigemAlteracaoStatus.API);
            assertThat(alteracao.sagaId()).isNull();
            assertThat(alteracao.ocorridoEm()).isEqualTo(ordem.getSituacaoAlteradaEm());
        });
    }

    @Test
    void registraTodaMudancaDeStatusMesmoDentroDaMesmaSituacao() {
        OrdemDeServico ordem = abrir();
        ordem.descarregarAlteracoesDeStatus();

        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico();

        assertThat(ordem.descarregarAlteracoesDeStatus())
                .extracting(AlteracaoStatusOrdemDeServico::statusAnterior, AlteracaoStatusOrdemDeServico::statusNovo)
                .containsExactly(
                        tuple(StatusOrdemDeServico.OS_ABERTA, StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO),
                        tuple(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO));
    }

    @Test
    void descarregarEsvaziaAsPendentes() {
        OrdemDeServico ordem = abrir();

        assertThat(ordem.descarregarAlteracoesDeStatus()).hasSize(1);
        assertThat(ordem.descarregarAlteracoesDeStatus()).isEmpty();
    }

    @Test
    void origemRegistradaValeParaAsProximasMudancas() {
        OrdemDeServico ordem = abrir();
        UUID sagaId = UUID.randomUUID();

        ordem.registrarOrigemDasAlteracoes(OrigemAlteracaoStatus.SAGA, sagaId);
        ordem.iniciarDiagnostico();

        assertThat(ordem.descarregarAlteracoesDeStatus())
                .extracting(AlteracaoStatusOrdemDeServico::origem, AlteracaoStatusOrdemDeServico::sagaId)
                .containsExactly(
                        tuple(OrigemAlteracaoStatus.API, null),
                        tuple(OrigemAlteracaoStatus.SAGA, sagaId));
    }

    @Test
    void origemEhObrigatoria() {
        OrdemDeServico ordem = abrir();

        assertThatThrownBy(() -> ordem.registrarOrigemDasAlteracoes(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ordemReconstituidaNaoTemPendencias() {
        OrdemDeServico aberta = abrir();
        OrdemDeServico reconstituida = OrdemDeServico.reconstituir(
                UUID.randomUUID(), "OS-HIST-2", aberta.getFuncionario(), aberta.getCliente(), aberta.getVeiculo(),
                StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, null, null, null);

        assertThat(reconstituida.descarregarAlteracoesDeStatus()).isEmpty();
    }

    private static OrdemDeServico abrir() {
        return OrdemDeServico.abrir(
                UUID.fromString("93333333-3333-3333-3333-333333333333"),
                "OS-HIST-1",
                Funcionario.reconstituir(UUID.fromString("94444444-4444-4444-4444-444444444444"), "Joao", null),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.fromString("92222222-2222-2222-2222-222222222222"),
                        CLIENTE_ID,
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
    }
}
