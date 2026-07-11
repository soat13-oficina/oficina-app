package br.com.oficina.ordemservico.application.command;

import java.util.List;

public record ConcluirDiagnosticoCommand(
        String numeroOrdemServico,
        String descricaoServico,
        List<PecaDiagnosticoInput> pecas) {

    public ConcluirDiagnosticoCommand(String numeroOrdemServico) {
        this(numeroOrdemServico, null, List.of());
    }

    public List<PecaDiagnosticoInput> pecas() {
        return pecas == null ? List.of() : pecas;
    }

    public boolean temDadosDeDiagnostico() {
        return descricaoServico != null && !descricaoServico.isBlank();
    }

    public record PecaDiagnosticoInput(String pecaInsumoId, int quantidade) {
    }
}
