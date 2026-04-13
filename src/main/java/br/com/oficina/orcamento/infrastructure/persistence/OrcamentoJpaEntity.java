package br.com.oficina.orcamento.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.oficina.orcamento.domain.model.Orcamento;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orcamentos")
public class OrcamentoJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String ordemDeServicoId;

    @Column(nullable = false)
    private String funcionarioId;

    @Column(nullable = false)
    private String clienteId;

    @Column(nullable = false)
    private String placaVeiculo;

    @Column(nullable = false, length = 4000)
    private String descricaoDiagnostico;

    @ElementCollection
    @CollectionTable(name = "orcamento_servicos_propostos", joinColumns = @JoinColumn(name = "orcamento_id"))
    @Column(name = "servico", nullable = false)
    private List<String> servicosPropostos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "orcamento_pecas_previstas", joinColumns = @JoinColumn(name = "orcamento_id"))
    @Column(name = "peca", nullable = false)
    private List<String> pecasPrevistas = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMaoDeObra;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPecas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime validade;

    @Column(length = 4000)
    private String observacoes;

    private LocalDateTime enviadoParaAprovacaoEm;

    protected OrcamentoJpaEntity() {
    }

    private OrcamentoJpaEntity(
            String id,
            String ordemDeServicoId,
            String funcionarioId,
            String clienteId,
            String placaVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<String> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal valorPecas,
            BigDecimal valorTotal,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            LocalDateTime enviadoParaAprovacaoEm) {
        this.id = id;
        this.ordemDeServicoId = ordemDeServicoId;
        this.funcionarioId = funcionarioId;
        this.clienteId = clienteId;
        this.placaVeiculo = placaVeiculo;
        this.descricaoDiagnostico = descricaoDiagnostico;
        this.servicosPropostos = new ArrayList<>(servicosPropostos);
        this.pecasPrevistas = new ArrayList<>(pecasPrevistas);
        this.valorMaoDeObra = valorMaoDeObra;
        this.valorPecas = valorPecas;
        this.valorTotal = valorTotal;
        this.criadoEm = criadoEm;
        this.validade = validade;
        this.observacoes = observacoes;
        this.enviadoParaAprovacaoEm = enviadoParaAprovacaoEm;
    }

    public static OrcamentoJpaEntity fromDomain(Orcamento orcamento) {
        return new OrcamentoJpaEntity(
                orcamento.getId(),
                orcamento.getOrdemDeServicoId(),
                orcamento.getFuncionarioId(),
                orcamento.getClienteId(),
                orcamento.getPlacaVeiculo(),
                orcamento.getDescricaoDiagnostico(),
                orcamento.getServicosPropostos(),
                orcamento.getPecasPrevistas(),
                orcamento.getValorMaoDeObra(),
                orcamento.getValorPecas(),
                orcamento.getValorTotal(),
                orcamento.getCriadoEm(),
                orcamento.getValidade(),
                orcamento.getObservacoes(),
                orcamento.getEnviadoParaAprovacaoEm());
    }

    public Orcamento toDomain() {
        Orcamento orcamento = new Orcamento(
                id,
                ordemDeServicoId,
                funcionarioId,
                clienteId,
                placaVeiculo,
                descricaoDiagnostico,
                servicosPropostos,
                pecasPrevistas,
                valorMaoDeObra,
                valorPecas,
                criadoEm,
                validade,
                observacoes);
        if (enviadoParaAprovacaoEm != null) {
            orcamento.enviarParaAprovacao(enviadoParaAprovacaoEm);
        }
        return orcamento;
    }
}
