package br.com.oficina.ordemservico.saga.infrastructure.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.oficina.ordemservico.saga.application.ConsultarSagaUseCase.SagaConsultada;
import br.com.oficina.ordemservico.saga.domain.model.PassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SagaResponse", description = "Estado atual e história da Saga de uma ordem de serviço")
public record SagaResponse(
        @Schema(description = "Id da Saga") UUID sagaId,
        @Schema(description = "Id da ordem de serviço") UUID ordemServicoId,
        @Schema(description = "Estado da Saga", example = "AGUARDANDO_DECISAO") StatusSaga status,
        @Schema(description = "Passo de negócio atual", example = "DECISAO") PassoSaga passoAtual,
        @Schema(description = "Orçamento em jogo, quando houver") UUID orcamentoId,
        @Schema(description = "Estado final das compensações em andamento", example = "ENCERRADA_SEM_EXECUCAO") StatusSaga desfecho,
        @Schema(description = "Motivo do encerramento ou da compensação", example = "ORCAMENTO_EXPIRADO") String motivo,
        @Schema(description = "Confirmações de compensação que faltam chegar") Set<String> compensacoesPendentes,
        @Schema(description = "Prazo em vigor (decisão do cliente ou resposta de compensação)") LocalDateTime prazoExpiraEm,
        @Schema(description = "Reenvios já feitos da compensação pendente") int tentativas,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        @Schema(description = "Passos registrados, do mais antigo para o mais recente") List<Passo> passos) {

    @Schema(name = "PassoSaga", description = "Um registro da história da Saga")
    public record Passo(
            PassoSaga passo,
            @Schema(example = "COMANDO_ENVIADO") RegistroPassoSaga.Tipo tipo,
            @Schema(description = "messageId do comando ou evento, quando houver") UUID mensagemId,
            String detalhe,
            LocalDateTime ocorridoEm) {
    }

    public static SagaResponse from(SagaConsultada consultada) {
        SagaOrdemServico saga = consultada.saga();
        return new SagaResponse(
                saga.getId(),
                saga.getOrdemDeServicoId(),
                saga.getStatus(),
                saga.getPassoAtual(),
                saga.getOrcamentoId(),
                saga.getDesfecho(),
                saga.getMotivo(),
                saga.getCompensacoesPendentes(),
                saga.getPrazoExpiraEm(),
                saga.getTentativasPasso(),
                saga.getCriadaEm(),
                saga.getAtualizadaEm(),
                consultada.passos().stream()
                        .map(p -> new Passo(p.getPasso(), p.getTipo(), p.getMensagemId(), p.getDetalhe(), p.getOcorridoEm()))
                        .toList());
    }
}
