package br.com.oficina.orcamento.application.command;

import br.com.oficina.orcamento.domain.model.CanalDecisao;

public record RejeitarOrcamentoCommand(String numeroOrcamento, CanalDecisao canal) {

    public RejeitarOrcamentoCommand(String numeroOrcamento) {
        this(numeroOrcamento, CanalDecisao.FUNCIONARIO);
    }
}
