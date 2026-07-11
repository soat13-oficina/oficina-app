package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;

import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StatusOrdemDeServicoResponse", description = "Situação de negócio atual de uma ordem de serviço")
public record StatusOrdemDeServicoResponse(
        @Schema(description = "Número da ordem de serviço", example = "OS-1A2B3C4D")
        String numeroOrdemServico,
        @Schema(description = "Situação de negócio canônica", example = "Aguardando Aprovação")
        String situacao,
        @Schema(description = "Motivo de encerramento (apenas quando Finalizada)", example = "SERVICO_CONCLUIDO")
        MotivoEncerramento motivoEncerramento,
        @Schema(description = "Nome do cliente", example = "Maria da Silva")
        String nomeCliente,
        @Schema(description = "Documento do cliente", example = "12345678909")
        String documentoCliente,
        @Schema(description = "Placa do veículo", example = "ABC1D23")
        String placaVeiculo,
        @Schema(description = "Início do diagnóstico")
        LocalDateTime iniciadaEm,
        @Schema(description = "Finalização da ordem")
        LocalDateTime finalizadaEm,
        @Schema(description = "Entrega ao cliente")
        LocalDateTime entregueEm) {

    public static StatusOrdemDeServicoResponse from(OrdemDeServico ordemDeServico) {
        return new StatusOrdemDeServicoResponse(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getSituacao().getDescricao(),
                ordemDeServico.getMotivoEncerramento(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getCliente().getCpfOuCnpj(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getIniciadaEm(),
                ordemDeServico.getFinalizadaEm(),
                ordemDeServico.getEntregueEm());
    }
}
