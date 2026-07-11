package br.com.oficina.ordemservico.infrastructure.web.request;

import java.util.List;

import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand.PecaDiagnosticoInput;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConcluirDiagnosticoRequest",
        description = "Dados do diagnóstico informados no fechamento: descrição do serviço e peças a usar.")
public record ConcluirDiagnosticoRequest(
        @Schema(description = "Descrição do serviço a executar", example = "Troca de pastilhas dianteiras e traseiras")
        String descricaoServico,
        @Schema(description = "Peças previstas (referência ao cadastro de peças/insumos)")
        List<PecaDiagnosticoRequest> pecas) {

    public List<PecaDiagnosticoInput> toPecas() {
        if (pecas == null) {
            return List.of();
        }
        return pecas.stream()
                .map(p -> new PecaDiagnosticoInput(p.pecaInsumoId(), p.quantidade()))
                .toList();
    }

    @Schema(name = "PecaDiagnosticoRequest", description = "Peça/insumo a usar e quantidade")
    public record PecaDiagnosticoRequest(String pecaInsumoId, int quantidade) {
    }
}
