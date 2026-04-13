package br.com.oficina.orcamento.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.oficina.orcamento.domain.model.Orcamento;

public record OrcamentoResponse(
        UUID id,
        String numeroOrcamento,
        ClienteOrcamentoResponse cliente,
        VeiculoOrcamentoResponse veiculo,
        DetalhesServicoResponse detalhesServico,
        String status,
        LocalDateTime criadoEm,
        LocalDateTime enviadoParaAprovacaoEm) {
    public static OrcamentoResponse from(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getNumeroOrcamento(),
                new ClienteOrcamentoResponse(orcamento.getClienteNome(), orcamento.getClienteCpf()),
                new VeiculoOrcamentoResponse(orcamento.getPlacaVeiculo(), orcamento.getMarcaVeiculo(), orcamento.getModeloVeiculo()),
                new DetalhesServicoResponse(
                        orcamento.getDescricaoDiagnostico(),
                        orcamento.getServicosPropostos(),
                        orcamento.getPecasPrevistas(),
                        orcamento.getValorMaoDeObra(),
                        orcamento.getValorPecas(),
                        orcamento.getValorTotal(),
                        orcamento.getValidade(),
                        orcamento.getObservacoes()),
                orcamento.getStatus().name(),
                orcamento.getCriadoEm(),
                orcamento.getEnviadoParaAprovacaoEm());
    }

    public record ClienteOrcamentoResponse(String nome, String cpf) {
    }

    public record VeiculoOrcamentoResponse(String placa, String marca, String modelo) {
    }

    public record DetalhesServicoResponse(
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<String> pecasPrevistas,
            java.math.BigDecimal valorMaoDeObra,
            java.math.BigDecimal valorPecas,
            java.math.BigDecimal valorTotal,
            LocalDateTime validade,
            String observacoes) {
    }
}
