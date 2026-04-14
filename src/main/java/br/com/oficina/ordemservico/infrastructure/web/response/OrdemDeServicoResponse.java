package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;

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
        LocalDateTime finalizadaEm) {
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
                ordemDeServico.getFinalizadaEm());
    }
}
