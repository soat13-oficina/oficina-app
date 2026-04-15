package br.com.oficina.ordemservico.infrastructure.web.response;

import java.math.BigDecimal;
import java.util.List;

import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.FinalizacaoOrdemDeServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FinalizacaoOrdemDeServicoResponse", description = "Resumo do fechamento de uma ordem de serviço")
public record FinalizacaoOrdemDeServicoResponse(
        String numeroOrdemServico,
        ClienteResponse cliente,
        VeiculoResponse veiculo,
        String tempoExecucao,
        String servicoRealizado,
        BigDecimal valorServico,
        List<PecaResponse> pecas,
        BigDecimal valorFinal,
        BigDecimal desconto) {
    @Override
    @Schema(description = "Número identificador da ordem de serviço", example = "OS-2026-0001")
    public String numeroOrdemServico() {
        return numeroOrdemServico;
    }

    @Override
    @Schema(description = "Dados do cliente vinculado à ordem de serviço")
    public ClienteResponse cliente() {
        return cliente;
    }

    @Override
    @Schema(description = "Dados do veículo vinculado à ordem de serviço")
    public VeiculoResponse veiculo() {
        return veiculo;
    }

    @Override
    @Schema(description = "Tempo total de execução calculado no fechamento", example = "1 dia e 3 horas")
    public String tempoExecucao() {
        return tempoExecucao;
    }

    @Override
    @Schema(description = "Descrição consolidada do serviço realizado", example = "Troca de óleo, substituição do filtro de óleo")
    public String servicoRealizado() {
        return servicoRealizado;
    }

    @Override
    @Schema(description = "Valor do serviço sem considerar peças", example = "200.00")
    public BigDecimal valorServico() {
        return valorServico;
    }

    @Override
    @Schema(description = "Peças aplicadas na execução do serviço")
    public List<PecaResponse> pecas() {
        return pecas;
    }

    @Override
    @Schema(description = "Valor final considerando serviço, peças e desconto", example = "365.90")
    public BigDecimal valorFinal() {
        return valorFinal;
    }

    @Override
    @Schema(description = "Desconto aplicado ao fechamento", example = "25.00")
    public BigDecimal desconto() {
        return desconto;
    }

    public static FinalizacaoOrdemDeServicoResponse from(FinalizacaoOrdemDeServico finalizacao) {
        return new FinalizacaoOrdemDeServicoResponse(
                finalizacao.numeroOrdemServico(),
                new ClienteResponse(
                        finalizacao.cliente().id(),
                        finalizacao.cliente().nome(),
                        finalizacao.cliente().documento()),
                new VeiculoResponse(
                        finalizacao.veiculo().id(),
                        finalizacao.veiculo().placa(),
                        finalizacao.veiculo().marca(),
                        finalizacao.veiculo().modelo()),
                finalizacao.tempoExecucao(),
                finalizacao.servicoRealizado(),
                finalizacao.valorServico(),
                finalizacao.pecas().stream().map(peca -> new PecaResponse(peca.descricao(), peca.preco())).toList(),
                finalizacao.valorFinal(),
                finalizacao.desconto());
    }

    @Schema(name = "FinalizacaoClienteResponse", description = "Dados do cliente no fechamento da OS")
    public record ClienteResponse(String id, String nome, String documento) {
        @Override
        @Schema(description = "Identificador do cliente", example = "11111111-1111-1111-1111-111111111111")
        public String id() {
            return id;
        }

        @Override
        @Schema(description = "Nome do cliente", example = "Maria da Silva")
        public String nome() {
            return nome;
        }

        @Override
        @Schema(description = "Documento do cliente", example = "12345678901")
        public String documento() {
            return documento;
        }
    }

    @Schema(name = "FinalizacaoVeiculoResponse", description = "Dados do veículo no fechamento da OS")
    public record VeiculoResponse(String id, String placa, String marca, String modelo) {
        @Override
        @Schema(description = "Identificador do veículo", example = "22222222-2222-2222-2222-222222222222")
        public String id() {
            return id;
        }

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

    @Schema(name = "FinalizacaoPecaResponse", description = "Peça utilizada no fechamento da OS")
    public record PecaResponse(String descricao, BigDecimal preco) {
        @Override
        @Schema(description = "Descrição da peça utilizada", example = "Filtro de óleo")
        public String descricao() {
            return descricao;
        }

        @Override
        @Schema(description = "Preço da peça utilizada", example = "45.90")
        public BigDecimal preco() {
            return preco;
        }
    }
}
