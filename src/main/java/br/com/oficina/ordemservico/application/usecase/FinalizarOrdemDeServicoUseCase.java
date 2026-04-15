package br.com.oficina.ordemservico.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;

public interface FinalizarOrdemDeServicoUseCase {
    FinalizacaoOrdemDeServico finalizarOrdemDeServico(FinalizarOrdemDeServicoCommand command);

    record FinalizacaoOrdemDeServico(
            String numeroOrdemServico,
            ClienteFinalizacao cliente,
            VeiculoFinalizacao veiculo,
            String tempoExecucao,
            String servicoRealizado,
            BigDecimal valorServico,
            List<PecaFinalizacao> pecas,
            BigDecimal valorFinal,
            BigDecimal desconto) {
    }

    record ClienteFinalizacao(String id, String nome, String documento) {
    }

    record VeiculoFinalizacao(String id, String placa, String marca, String modelo) {
    }

    record PecaFinalizacao(String descricao, BigDecimal preco) {
    }
}
