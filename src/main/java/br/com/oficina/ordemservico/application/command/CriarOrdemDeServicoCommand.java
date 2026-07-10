package br.com.oficina.ordemservico.application.command;

import java.math.BigDecimal;
import java.util.List;

public record CriarOrdemDeServicoCommand(
        String clienteId,
        String funcionarioId,
        String placaVeiculo,
        List<ServicoItem> servicos,
        List<PecaItem> pecasPrevistas) {

    public CriarOrdemDeServicoCommand(String clienteId, String funcionarioId, String placaVeiculo) {
        this(clienteId, funcionarioId, placaVeiculo, List.of(), List.of());
    }

    public List<ServicoItem> servicos() {
        return servicos == null ? List.of() : servicos;
    }

    public List<PecaItem> pecasPrevistas() {
        return pecasPrevistas == null ? List.of() : pecasPrevistas;
    }

    public record ServicoItem(String descricao, BigDecimal valorMaoDeObra) {
    }

    public record PecaItem(String pecaInsumoId, int quantidade) {
    }
}
