package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrdemDeServicoResponse", description = "Representação de uma ordem de serviço")
public record OrdemDeServicoResponse(
        UUID id,
        String numeroOrdemServico,
        UUID funcionarioId,
        String clienteId,
        UUID veiculoId,
        String nomeCliente,
        String documentoCliente,
        TipoCliente tipoCliente,
        String placaVeiculo,
        StatusOrdemDeServico status,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm,
        LocalDateTime entregueEm) {
    @Override
    @Schema(description = "Identificador UUID da ordem de serviço", example = "33333333-3333-3333-3333-333333333333")
    public UUID id() {
        return id;
    }

    @Override
    @Schema(description = "Número da ordem de serviço", example = "OS-1A2B3C4D")
    public String numeroOrdemServico() {
        return numeroOrdemServico;
    }

    @Override
    @Schema(description = "Identificador UUID do funcionário responsável", example = "22222222-2222-2222-2222-222222222222")
    public UUID funcionarioId() {
        return funcionarioId;
    }

    @Override
    @Schema(description = "Identificador UUID do cliente", example = "11111111-1111-1111-1111-111111111111")
    public String clienteId() {
        return clienteId;
    }

    @Override
    @Schema(description = "Identificador UUID do veículo", example = "44444444-4444-4444-4444-444444444444")
    public UUID veiculoId() {
        return veiculoId;
    }

    @Override
    @Schema(description = "Nome do cliente vinculado", example = "Maria da Silva")
    public String nomeCliente() {
        return nomeCliente;
    }

    @Override
    @Schema(description = "CPF ou CNPJ do cliente vinculado", example = "12345678901")
    public String documentoCliente() {
        return documentoCliente;
    }

    @Override
    @Schema(description = "Tipo do cliente vinculado", example = "PF")
    public TipoCliente tipoCliente() {
        return tipoCliente;
    }

    @Override
    @Schema(description = "Placa do veículo vinculado", example = "ABC1D23")
    public String placaVeiculo() {
        return placaVeiculo;
    }

    @Override
    @Schema(description = "Status atual da ordem de serviço", example = "ABERTA")
    public StatusOrdemDeServico status() {
        return status;
    }

    @Override
    @Schema(description = "Data e hora de início do diagnóstico", example = "2030-01-01T10:00:00")
    public LocalDateTime iniciadaEm() {
        return iniciadaEm;
    }

    @Override
    @Schema(description = "Data e hora de finalização da ordem", example = "2030-01-02T18:30:00")
    public LocalDateTime finalizadaEm() {
        return finalizadaEm;
    }

    @Override
    @Schema(description = "Data e hora de entrega da ordem ao cliente", example = "2030-01-03T09:15:00")
    public LocalDateTime entregueEm() {
        return entregueEm;
    }

    public static OrdemDeServicoResponse from(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoResponse(
                ordemDeServico.getId(),
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getFuncionario().getId(),
                ordemDeServico.getCliente().getId().toString(),
                ordemDeServico.getVeiculoId(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getCliente().getCpfOuCnpj(),
                ordemDeServico.getCliente().getTipoCliente(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getStatus(),
                ordemDeServico.getIniciadaEm(),
                ordemDeServico.getFinalizadaEm(),
                ordemDeServico.getEntregueEm());
    }
}
