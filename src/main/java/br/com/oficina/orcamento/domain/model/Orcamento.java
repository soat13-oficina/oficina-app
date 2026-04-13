package br.com.oficina.orcamento.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Orcamento {
    private final String numeroOrcamento;
    private final String ordemDeServicoId;
    private final String funcionarioId;
    private final String clienteNome;
    private final String clienteCpf;
    private final String placaVeiculo;
    private final String marcaVeiculo;
    private final String modeloVeiculo;
    private final String descricaoDiagnostico;
    private final List<String> servicosPropostos;
    private final List<String> pecasPrevistas;
    private final BigDecimal valorMaoDeObra;
    private final BigDecimal valorPecas;
    private final BigDecimal valorTotal;
    private final LocalDateTime criadoEm;
    private final LocalDateTime validade;
    private final String observacoes;
    private StatusOrcamento status;
    private LocalDateTime enviadoParaAprovacaoEm;

    public Orcamento(
            String numeroOrcamento,
            String ordemDeServicoId,
            String funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<String> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal valorPecas,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        this.numeroOrcamento = numeroOrcamento;
        this.ordemDeServicoId = ordemDeServicoId;
        this.funcionarioId = funcionarioId;
        this.clienteNome = clienteNome;
        this.clienteCpf = clienteCpf;
        this.placaVeiculo = placaVeiculo;
        this.marcaVeiculo = marcaVeiculo;
        this.modeloVeiculo = modeloVeiculo;
        this.descricaoDiagnostico = descricaoDiagnostico;
        this.servicosPropostos = List.copyOf(servicosPropostos);
        this.pecasPrevistas = List.copyOf(pecasPrevistas);
        this.valorMaoDeObra = valorMaoDeObra;
        this.valorPecas = valorPecas;
        this.valorTotal = valorMaoDeObra.add(valorPecas);
        this.criadoEm = criadoEm;
        this.validade = validade;
        this.observacoes = observacoes;
        this.status = status;
    }

    public void enviarParaAprovacao(LocalDateTime enviadoParaAprovacaoEm) {
        this.enviadoParaAprovacaoEm = enviadoParaAprovacaoEm;
        this.status = StatusOrcamento.AGUARDANDO_APROVACAO;
    }

    public void aprovar() {
        this.status = StatusOrcamento.APROVADO;
    }

    public void rejeitar() {
        this.status = StatusOrcamento.REJEITADO;
    }

    public String getNumeroOrcamento() {
        return numeroOrcamento;
    }

    public String getOrdemDeServicoId() {
        return ordemDeServicoId;
    }

    public String getFuncionarioId() {
        return funcionarioId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteCpf() {
        return clienteCpf;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public String getMarcaVeiculo() {
        return marcaVeiculo;
    }

    public String getModeloVeiculo() {
        return modeloVeiculo;
    }

    public String getDescricaoDiagnostico() {
        return descricaoDiagnostico;
    }

    public List<String> getServicosPropostos() {
        return servicosPropostos;
    }

    public List<String> getPecasPrevistas() {
        return pecasPrevistas;
    }

    public BigDecimal getValorMaoDeObra() {
        return valorMaoDeObra;
    }

    public BigDecimal getValorPecas() {
        return valorPecas;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getValidade() {
        return validade;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public LocalDateTime getEnviadoParaAprovacaoEm() {
        return enviadoParaAprovacaoEm;
    }
}
