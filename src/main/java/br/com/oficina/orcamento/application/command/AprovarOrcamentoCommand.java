package br.com.oficina.orcamento.application.command;

import br.com.oficina.orcamento.domain.model.CanalDecisao;

public record AprovarOrcamentoCommand(String numeroOrcamento, CanalDecisao canal) {

    public AprovarOrcamentoCommand(String numeroOrcamento) {
        this(numeroOrcamento, CanalDecisao.FUNCIONARIO);
    }
}
