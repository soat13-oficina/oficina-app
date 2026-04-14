package br.com.oficina.orcamento.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.oficina.orcamento.domain.model.Orcamento;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrcamentoResponse", description = "Representação de um orçamento")
public record OrcamentoResponse(
        UUID id,
        String numeroOrcamento,
        ClienteOrcamentoResponse cliente,
        VeiculoOrcamentoResponse veiculo,
        DetalhesServicoResponse detalhesServico,
        String status,
        LocalDateTime criadoEm,
        LocalDateTime enviadoParaAprovacaoEm) {
    @Override
    @Schema(description = "Identificador UUID do orçamento", example = "55555555-5555-5555-5555-555555555555")
    public UUID id() {
        return id;
    }

    @Override
    @Schema(description = "Número do orçamento", example = "ORC-2026-0001")
    public String numeroOrcamento() {
        return numeroOrcamento;
    }

    @Override
    @Schema(description = "Dados do cliente vinculado")
    public ClienteOrcamentoResponse cliente() {
        return cliente;
    }

    @Override
    @Schema(description = "Dados do veículo vinculado")
    public VeiculoOrcamentoResponse veiculo() {
        return veiculo;
    }

    @Override
    @Schema(description = "Detalhes técnicos e financeiros do orçamento")
    public DetalhesServicoResponse detalhesServico() {
        return detalhesServico;
    }

    @Override
    @Schema(description = "Status atual do orçamento", example = "RASCUNHO")
    public String status() {
        return status;
    }

    @Override
    @Schema(description = "Data e hora de criação do orçamento", example = "2030-01-01T10:00:00")
    public LocalDateTime criadoEm() {
        return criadoEm;
    }

    @Override
    @Schema(description = "Data e hora de envio para aprovação", example = "2030-01-01T11:00:00")
    public LocalDateTime enviadoParaAprovacaoEm() {
        return enviadoParaAprovacaoEm;
    }

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

    @Schema(name = "ClienteOrcamentoResponse", description = "Dados do cliente vinculados ao orçamento")
    public record ClienteOrcamentoResponse(String nome, String cpf) {
        @Override
        @Schema(description = "Nome do cliente", example = "Maria da Silva")
        public String nome() {
            return nome;
        }

        @Override
        @Schema(description = "CPF do cliente", example = "12345678901")
        public String cpf() {
            return cpf;
        }
    }

    @Schema(name = "VeiculoOrcamentoResponse", description = "Dados do veículo vinculados ao orçamento")
    public record VeiculoOrcamentoResponse(String placa, String marca, String modelo) {
        @Override
        @Schema(description = "Placa do veículo", example = "ABC1D23")
        public String placa() {
            return placa;
        }

        @Override
        @Schema(description = "Marca do veículo", example = "Toyota")
        public String marca() {
            return marca;
        }

        @Override
        @Schema(description = "Modelo do veículo", example = "Corolla")
        public String modelo() {
            return modelo;
        }
    }

    @Schema(name = "DetalhesServicoResponse", description = "Detalhes técnicos e financeiros do orçamento")
    public record DetalhesServicoResponse(
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<String> pecasPrevistas,
            java.math.BigDecimal valorMaoDeObra,
            java.math.BigDecimal valorPecas,
            java.math.BigDecimal valorTotal,
            LocalDateTime validade,
            String observacoes) {
        @Override
        @Schema(description = "Descrição do diagnóstico técnico", example = "Troca de óleo e revisão de freios")
        public String descricaoDiagnostico() {
            return descricaoDiagnostico;
        }

        @Override
        @Schema(description = "Lista de serviços propostos")
        public List<String> servicosPropostos() {
            return servicosPropostos;
        }

        @Override
        @Schema(description = "Lista de peças previstas")
        public List<String> pecasPrevistas() {
            return pecasPrevistas;
        }

        @Override
        @Schema(description = "Valor estimado da mão de obra", example = "200.00")
        public java.math.BigDecimal valorMaoDeObra() {
            return valorMaoDeObra;
        }

        @Override
        @Schema(description = "Valor estimado das peças", example = "150.00")
        public java.math.BigDecimal valorPecas() {
            return valorPecas;
        }

        @Override
        @Schema(description = "Valor total do orçamento", example = "350.00")
        public java.math.BigDecimal valorTotal() {
            return valorTotal;
        }

        @Override
        @Schema(description = "Data de validade do orçamento", example = "2030-01-01T00:00:00")
        public LocalDateTime validade() {
            return validade;
        }

        @Override
        @Schema(description = "Observações adicionais", example = "Peças sujeitas à disponibilidade em estoque")
        public String observacoes() {
            return observacoes;
        }
    }
}
