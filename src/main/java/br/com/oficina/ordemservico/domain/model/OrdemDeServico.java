package br.com.oficina.ordemservico.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "ordens_de_servico")
public class OrdemDeServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numeroOrdemServico;

    @Column(nullable = false)
    private UUID funcionarioId;

    @Column(nullable = false)
    private String funcionarioNome;

    private String funcionarioCpf;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private UUID veiculoId;

    @Column(nullable = false)
    private String clienteNome;

    private String clienteDocumento;

    @Enumerated(EnumType.STRING)
    private TipoCliente clienteTipo;

    @Column(nullable = false)
    private String veiculoPlaca;

    @Column(nullable = false)
    private String veiculoMarca;

    @Column(nullable = false)
    private String veiculoModelo;

    @Column(nullable = false)
    private String veiculoFabricante;

    @Column(nullable = false)
    private int veiculoAno;

    @Column(nullable = false)
    private int veiculoPotencia;

    @Column(nullable = false)
    private String veiculoCambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCombustivel veiculoTipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOrdemDeServico status;

    private LocalDateTime iniciadaEm;

    private LocalDateTime finalizadaEm;

    private LocalDateTime entregueEm;

    // Quando a SITUACAO de negocio mudou pela ultima vez (migration V19). Distinto de
    // iniciadaEm/finalizadaEm/entregueEm: e o relogio da situacao CORRENTE, seja ela qual for,
    // e a base do "tempo medio por status" no dashboard.
    @Column(name = "situacao_alterada_em")
    private LocalDateTime situacaoAlteradaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_encerramento")
    private MotivoEncerramento motivoEncerramento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ordem_servico_servicos",
            joinColumns = @JoinColumn(name = "ordem_de_servico_id"))
    private List<ServicoOrdem> servicos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ordem_servico_pecas",
            joinColumns = @JoinColumn(name = "ordem_de_servico_id"))
    private List<PecaPrevistaOrdem> pecasPrevistas = new ArrayList<>();

    // Mudancas de status ainda nao gravadas no historico (tabela ordem_servico_historico). O
    // agregado so acumula; quem grava e o repositorio, no mesmo salvar() da ordem.
    @Transient
    private final List<AlteracaoStatusOrdemDeServico> alteracoesDeStatusPendentes = new ArrayList<>();

    // Origem atribuida as proximas mudancas de status. API por padrao; o orquestrador da Saga
    // troca para SAGA/TIMEOUT antes de mover a ordem (registrarOrigemDasAlteracoes).
    @Transient
    private OrigemAlteracaoStatus origemDasAlteracoes = OrigemAlteracaoStatus.API;

    @Transient
    private UUID sagaIdDasAlteracoes;

    protected OrdemDeServico() {
    }

    private OrdemDeServico(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.status = status;
        this.iniciadaEm = iniciadaEm;
        this.finalizadaEm = finalizadaEm;
        this.entregueEm = entregueEm;
        definirDados(funcionario, cliente, veiculo);
    }

    public static OrdemDeServico abrir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo) {
        OrdemDeServico ordemDeServico = new OrdemDeServico(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                StatusOrdemDeServico.OS_ABERTA,
                null,
                null,
                null);
        // Abrir a OS ja e a entrada em "Recebida": o relogio da situacao comeca aqui.
        ordemDeServico.situacaoAlteradaEm = LocalDateTime.now();
        ordemDeServico.registrarNoHistorico(null, StatusOrdemDeServico.OS_ABERTA, ordemDeServico.situacaoAlteradaEm);
        return ordemDeServico;
    }

    public static OrdemDeServico abrir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            List<ServicoOrdem> servicos,
            List<PecaPrevistaOrdem> pecasPrevistas) {
        OrdemDeServico ordemDeServico = abrir(id, numeroOrdemServico, funcionario, cliente, veiculo);
        if (servicos != null) {
            ordemDeServico.servicos.addAll(servicos);
        }
        if (pecasPrevistas != null) {
            ordemDeServico.pecasPrevistas.addAll(pecasPrevistas);
        }
        return ordemDeServico;
    }

    public static OrdemDeServico reconstituir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm) {
        return new OrdemDeServico(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                status,
                iniciadaEm,
                finalizadaEm,
                entregueEm);
    }

    public static OrdemDeServico reconstituir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm,
            MotivoEncerramento motivoEncerramento,
            List<ServicoOrdem> servicos,
            List<PecaPrevistaOrdem> pecasPrevistas) {
        OrdemDeServico ordemDeServico = reconstituir(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                status,
                iniciadaEm,
                finalizadaEm,
                entregueEm);
        ordemDeServico.motivoEncerramento = motivoEncerramento;
        if (servicos != null) {
            ordemDeServico.servicos.addAll(servicos);
        }
        if (pecasPrevistas != null) {
            ordemDeServico.pecasPrevistas.addAll(pecasPrevistas);
        }
        return ordemDeServico;
    }

    public void iniciarDiagnostico() {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Diagnostico so pode ser iniciado para ordem aberta");
        }
        alterarStatus(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        iniciadaEm = LocalDateTime.now();
    }

    public void alterar(Cliente cliente, Veiculo veiculo) {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser alterada enquanto estiver aberta");
        }
        definirDados(getFuncionario(), cliente, veiculo);
    }

    public void concluirDiagnostico() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        alterarStatus(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);
    }

    public void concluirDiagnostico(String descricaoServico, List<PecaPrevistaOrdem> pecas) {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        if (descricaoServico == null || descricaoServico.isBlank()) {
            throw new RegraDeNegocioException("Descricao do servico e obrigatoria no fechamento do diagnostico");
        }
        // Mao de obra nao e definida no fechamento (e financeiro do orcamento): registra apenas a descricao.
        // Diagnostico reaberto e fechado de novo com o mesmo servico nao o duplica.
        if (servicos.stream().noneMatch(servico -> servico.getDescricao().equals(descricaoServico))) {
            servicos.add(new ServicoOrdem(descricaoServico, null));
        }
        if (pecas != null) {
            pecasPrevistas.addAll(pecas);
        }
        alterarStatus(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);
    }

    /**
     * O orcamento nao pode ser gerado com as pecas do diagnostico (peca inexistente): o diagnostico
     * volta ao mecanico, que o fecha de novo informando as pecas. Chamado pela Saga ao receber
     * {@code OrcamentoFalhou}. Os servicos ficam; as pecas, que causaram a falha, saem.
     */
    public void reabrirDiagnostico() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser reaberto quando concluido");
        }
        pecasPrevistas.clear();
        alterarStatus(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
    }

    /** Orcamento gerado pelo Billing e enviado ao cliente (evento {@code OrcamentoGerado}). */
    public void enviarParaAprovacao() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser enviada para aprovacao com diagnostico concluido");
        }
        alterarStatus(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
    }

    /**
     * Orcamento aprovado e pecas reservadas: falta o pagamento. Chamado pelo orquestrador da Saga ao
     * receber {@code PecasReservadas} - quem aprova e o orcamento, a ordem so reage (ADR 0004).
     */
    public void aguardarPagamento() {
        if (status != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Ordem so pode aguardar pagamento depois da aprovacao do orcamento");
        }
        alterarStatus(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
    }

    /** Pagamento confirmado: a ordem entra na fila de execucao ({@code PagamentoConfirmado}). */
    public void confirmarPagamento() {
        if (status != StatusOrdemDeServico.AGUARDANDO_PAGAMENTO) {
            throw new RegraDeNegocioException("Pagamento so pode ser confirmado quando a ordem estiver aguardando pagamento");
        }
        alterarStatus(StatusOrdemDeServico.AGUARDANDO_EXECUCAO);
    }

    /** Pagamento recusado ou expirado: a Saga desfaz reserva e orcamento e a ordem e cancelada. */
    public void cancelarPorPagamento(MotivoEncerramento motivo) {
        if (motivo != MotivoEncerramento.PAGAMENTO_RECUSADO && motivo != MotivoEncerramento.PAGAMENTO_EXPIRADO) {
            throw new IllegalArgumentException("Motivo de cancelamento por pagamento invalido: " + motivo);
        }
        if (status != StatusOrdemDeServico.AGUARDANDO_PAGAMENTO) {
            throw new RegraDeNegocioException("Ordem so pode ser cancelada por pagamento quando estiver aguardando pagamento");
        }
        alterarStatus(StatusOrdemDeServico.CANCELADA);
        motivoEncerramento = motivo;
        finalizadaEm = LocalDateTime.now();
    }

    public void iniciarExecucao() {
        if (status != StatusOrdemDeServico.AGUARDANDO_EXECUCAO) {
            throw new RegraDeNegocioException("Execucao so pode iniciar quando a ordem estiver aguardando execucao");
        }
        alterarStatus(StatusOrdemDeServico.SERVICO_EM_ANDAMENTO);
    }

    public void concluirServico() {
        if (status != StatusOrdemDeServico.SERVICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Servico so pode ser concluido quando estiver em execucao");
        }
        alterarStatus(StatusOrdemDeServico.OS_FINALIZADA);
        motivoEncerramento = MotivoEncerramento.SERVICO_CONCLUIDO;
        finalizadaEm = LocalDateTime.now();
    }

    /** Mecanico abortou a execucao: a Saga estorna o pagamento e libera as pecas. */
    public void abortarExecucao() {
        if (status != StatusOrdemDeServico.SERVICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Execucao so pode ser abortada quando estiver em andamento");
        }
        alterarStatus(StatusOrdemDeServico.CANCELADA);
        motivoEncerramento = MotivoEncerramento.FALHA_EXECUCAO;
        finalizadaEm = LocalDateTime.now();
    }

    public void recusarOrcamento() {
        if (status != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode ser recusado quando a ordem estiver aguardando aprovacao");
        }
        alterarStatus(StatusOrdemDeServico.OS_FINALIZADA);
        motivoEncerramento = MotivoEncerramento.ORCAMENTO_RECUSADO;
        finalizadaEm = LocalDateTime.now();
    }

    /** Prazo de decisao da Saga vencido sem resposta do cliente. */
    public void expirarOrcamento() {
        if (status != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode expirar quando a ordem estiver aguardando aprovacao");
        }
        alterarStatus(StatusOrdemDeServico.OS_FINALIZADA);
        motivoEncerramento = MotivoEncerramento.ORCAMENTO_EXPIRADO;
        finalizadaEm = LocalDateTime.now();
    }

    public void entregarAoCliente() {
        if (status != StatusOrdemDeServico.OS_FINALIZADA) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser entregue ao cliente quando estiver finalizada");
        }
        alterarStatus(StatusOrdemDeServico.ENTREGUE);
        entregueEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public Funcionario getFuncionario() {
        return Funcionario.reconstituir(funcionarioId, funcionarioNome, funcionarioCpf);
    }

    public Cliente getCliente() {
        return Cliente.reconstituir(clienteId, clienteNome, clienteDocumento, clienteTipo);
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public Veiculo getVeiculo() {
        return Veiculo.reconstituir(
                veiculoId,
                clienteId,
                veiculoPlaca,
                veiculoMarca,
                veiculoModelo,
                veiculoFabricante,
                veiculoAno,
                veiculoPotencia,
                veiculoCambio,
                veiculoTipo);
    }

    public StatusOrdemDeServico getStatus() {
        return status;
    }

    public LocalDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public LocalDateTime getFinalizadaEm() {
        return finalizadaEm;
    }

    public LocalDateTime getEntregueEm() {
        return entregueEm;
    }

    public SituacaoOrdemDeServico getSituacao() {
        return SituacaoOrdemDeServico.fromStatus(status);
    }

    /**
     * Instante em que a ordem entrou na situacao em que esta agora. Pode ser {@code null} em
     * ordens anteriores a migration V19 que nunca sairam de "Recebida" (nao havia coluna de
     * criacao para o backfill usar).
     */
    public LocalDateTime getSituacaoAlteradaEm() {
        return situacaoAlteradaEm;
    }

    public MotivoEncerramento getMotivoEncerramento() {
        return motivoEncerramento;
    }

    public List<ServicoOrdem> getServicos() {
        return Collections.unmodifiableList(servicos);
    }

    public List<PecaPrevistaOrdem> getPecasPrevistas() {
        return Collections.unmodifiableList(pecasPrevistas);
    }

    /**
     * Define a origem das proximas mudancas de status desta instancia (vale ate o fim da unidade
     * de trabalho; uma ordem recarregada volta a {@link OrigemAlteracaoStatus#API}).
     */
    public void registrarOrigemDasAlteracoes(OrigemAlteracaoStatus origem, UUID sagaId) {
        if (origem == null) {
            throw new IllegalArgumentException("Origem da alteracao de status e obrigatoria");
        }
        this.origemDasAlteracoes = origem;
        this.sagaIdDasAlteracoes = sagaId;
    }

    /**
     * Entrega as mudancas de status acumuladas desde a ultima chamada e esvazia a lista. Usado
     * pelo repositorio para gravar o historico junto com a ordem.
     */
    public List<AlteracaoStatusOrdemDeServico> descarregarAlteracoesDeStatus() {
        List<AlteracaoStatusOrdemDeServico> pendentes = List.copyOf(alteracoesDeStatusPendentes);
        alteracoesDeStatusPendentes.clear();
        return pendentes;
    }

    /**
     * Unico ponto de escrita do status. Reinicia o relogio da situacao somente quando a
     * SITUACAO de negocio muda: DIAGNOSTICO_EM_ANDAMENTO -> DIAGNOSTICO_CONCLUIDO sao dois
     * status dentro de "Diagnostico", e zerar o contador ali quebraria o tempo medio da etapa.
     * O historico, ao contrario, registra toda mudanca de status.
     */
    private void alterarStatus(StatusOrdemDeServico novoStatus) {
        StatusOrdemDeServico statusAnterior = status;
        SituacaoOrdemDeServico situacaoAnterior = SituacaoOrdemDeServico.fromStatus(statusAnterior);
        LocalDateTime agora = LocalDateTime.now();
        status = novoStatus;
        if (SituacaoOrdemDeServico.fromStatus(novoStatus) != situacaoAnterior) {
            situacaoAlteradaEm = agora;
        }
        registrarNoHistorico(statusAnterior, novoStatus, agora);
    }

    private void registrarNoHistorico(StatusOrdemDeServico anterior, StatusOrdemDeServico novo, LocalDateTime ocorridoEm) {
        alteracoesDeStatusPendentes.add(
                new AlteracaoStatusOrdemDeServico(anterior, novo, origemDasAlteracoes, sagaIdDasAlteracoes, ocorridoEm));
    }

    private void definirDados(Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        if (cliente.getId() == null) {
            throw new RegraDeNegocioException("Cliente da ordem de servico deve possuir identificador.");
        }
        if (veiculo.getId() == null) {
            throw new RegraDeNegocioException("Veiculo da ordem de servico deve possuir identificador.");
        }
        this.funcionarioId = funcionario.getId();
        this.funcionarioNome = funcionario.getNome();
        this.funcionarioCpf = funcionario.getCpf();
        this.clienteId = cliente.getId();
        this.clienteNome = cliente.getNome();
        this.clienteDocumento = cliente.getCpfOuCnpj();
        this.clienteTipo = cliente.getTipoCliente();
        this.veiculoId = veiculo.getId();
        this.veiculoPlaca = veiculo.getPlaca();
        this.veiculoMarca = veiculo.getMarca();
        this.veiculoModelo = veiculo.getModelo();
        this.veiculoFabricante = veiculo.getFabricante();
        this.veiculoAno = veiculo.getAno();
        this.veiculoPotencia = veiculo.getPotencia();
        this.veiculoCambio = veiculo.getCambio();
        this.veiculoTipo = veiculo.getTipo();
    }
}
