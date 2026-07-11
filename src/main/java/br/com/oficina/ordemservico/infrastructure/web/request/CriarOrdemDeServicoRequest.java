package br.com.oficina.ordemservico.infrastructure.web.request;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CriarOrdemDeServicoRequest", description = "Dados necessários para abrir uma ordem de serviço")
public record CriarOrdemDeServicoRequest(
        @Schema(description = "Identificador UUID do cliente da ordem de serviço", example = "11111111-1111-1111-1111-111111111111")
        String clienteId,
        @Schema(description = "Identificador UUID do funcionário responsável pela abertura", example = "22222222-2222-2222-2222-222222222222")
        String funcionarioId,
        @Schema(description = "Placa do veículo vinculado à ordem de serviço", example = "ABC1D23")
        String placaVeiculo,
        @Schema(description = "Serviços previstos para a ordem de serviço")
        List<ServicoRequest> servicos,
        @Schema(description = "Peças previstas para a ordem de serviço")
        List<PecaRequest> pecasPrevistas) {

    @Schema(name = "ServicoOrdemRequest", description = "Serviço previsto na abertura da ordem")
    public record ServicoRequest(
            @Schema(description = "Descrição do serviço", example = "Troca de pastilhas de freio")
            String descricao,
            @Schema(description = "Valor da mão de obra", example = "150.00")
            BigDecimal valorMaoDeObra) {
    }

    @Schema(name = "PecaPrevistaRequest", description = "Peça prevista na abertura da ordem")
    public record PecaRequest(
            @Schema(description = "Identificador da peça/insumo", example = "REF-001")
            String pecaInsumoId,
            @Schema(description = "Quantidade prevista", example = "2")
            int quantidade) {
    }
}
