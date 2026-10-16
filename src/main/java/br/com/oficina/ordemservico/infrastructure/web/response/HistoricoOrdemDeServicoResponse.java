package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.oficina.ordemservico.application.usecase.ConsultarHistoricoOrdemDeServicoUseCase.HistoricoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrigemAlteracaoStatus;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HistoricoOrdemDeServicoResponse", description = "Situação atual e todas as mudanças de status de uma ordem de serviço")
public record HistoricoOrdemDeServicoResponse(
        @Schema(description = "Número da ordem de serviço", example = "OS-1A2B3C4D")
        String numeroOrdemServico,
        @Schema(description = "Situação de negócio atual", example = "Diagnóstico")
        String situacaoAtual,
        @Schema(description = "Mudanças de status, da mais antiga para a mais recente")
        List<Alteracao> alteracoes) {

    @Schema(name = "AlteracaoStatusOrdemDeServico", description = "Uma mudança de status da ordem de serviço")
    public record Alteracao(
            @Schema(description = "Status anterior (vazio na abertura)", example = "OS_ABERTA")
            StatusOrdemDeServico statusAnterior,
            @Schema(description = "Novo status", example = "DIAGNOSTICO_EM_ANDAMENTO")
            StatusOrdemDeServico statusNovo,
            @Schema(description = "Situação de negócio correspondente ao novo status", example = "Diagnóstico")
            String situacao,
            @Schema(description = "Quem provocou a mudança: API, SAGA, TIMEOUT ou MIGRACAO", example = "API")
            OrigemAlteracaoStatus origem,
            @Schema(description = "Saga que provocou a mudança, quando houver")
            UUID sagaId,
            @Schema(description = "Instante da mudança")
            LocalDateTime ocorridoEm) {

        static Alteracao from(AlteracaoStatusOrdemDeServico alteracao) {
            return new Alteracao(
                    alteracao.statusAnterior(),
                    alteracao.statusNovo(),
                    alteracao.situacaoNova().getDescricao(),
                    alteracao.origem(),
                    alteracao.sagaId(),
                    alteracao.ocorridoEm());
        }
    }

    public static HistoricoOrdemDeServicoResponse from(HistoricoOrdemDeServico historico) {
        return new HistoricoOrdemDeServicoResponse(
                historico.ordemDeServico().getNumeroOrdemServico(),
                historico.ordemDeServico().getSituacao().getDescricao(),
                historico.alteracoes().stream().map(Alteracao::from).toList());
    }
}
