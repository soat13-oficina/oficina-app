package br.com.oficina.orcamento.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Orcamento {
    private final String id;
    private final String ordemDeServicoId;
    private final String funcionarioId;
    private final String clienteId;
    private final String placaVeiculo;
    private final String descricaoDiagnostico;
    private final List<String> servicosPropostos;
    private final List<String> pecasPrevistas;
    private final BigDecimal valorMaoDeObra;
    private final BigDecimal valorPecas;
    private final BigDecimal valorTotal;
    private final LocalDateTime criadoEm;
    private final LocalDateTime validade;
    private final String observacoes;
    private LocalDateTime enviadoParaAprovacaoEm;

    public Orcamento(
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
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes) {
        this.id = id;
        this.ordemDeServicoId = ordemDeServicoId;
        this.funcionarioId = funcionarioId;
        this.clienteId = clienteId;
        this.placaVeiculo = placaVeiculo;
        this.descricaoDiagnostico = descricaoDiagnostico;
        this.servicosPropostos = List.copyOf(servicosPropostos);
        this.pecasPrevistas = List.copyOf(pecasPrevistas);
        this.valorMaoDeObra = valorMaoDeObra;
        this.valorPecas = valorPecas;
        this.valorTotal = valorMaoDeObra.add(valorPecas);
        this.criadoEm = criadoEm;
        this.validade = validade;
        this.observacoes = observacoes;
    }

    public void enviarParaAprovacao(LocalDateTime enviadoParaAprovacaoEm) {
        this.enviadoParaAprovacaoEm = enviadoParaAprovacaoEm;
    }

    public String getId() {
        return id;
    }

    public String getOrdemDeServicoId() {
        return ordemDeServicoId;
    }

    public String getFuncionarioId() {
        return funcionarioId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
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

    public LocalDateTime getEnviadoParaAprovacaoEm() {
        return enviadoParaAprovacaoEm;
    }
}
