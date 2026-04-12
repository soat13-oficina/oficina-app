package br.com.oficina.ordemservico.domain.model;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.Veiculo;

public class OrdemDeServico {
    private final String id;
    private final String numeroOrdemServico;
    private final Funcionario funcionario;
    private final Cliente cliente;
    private final Veiculo veiculo;
    private StatusOrdemDeServico status = StatusOrdemDeServico.ABERTA;

    private OrdemDeServico(
            String id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.funcionario = funcionario;
        this.cliente = cliente;
        this.veiculo = veiculo;
    }

    public static OrdemDeServico abrir(
            String id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo) {
        return new OrdemDeServico(id, numeroOrdemServico, funcionario, cliente, veiculo);
    }

    public void iniciarDiagnostico() {
        if (status != StatusOrdemDeServico.ABERTA) {
            throw new RegraDeNegocioException("Diagnostico so pode ser iniciado para ordem aberta");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO;
    }

    public void concluirDiagnostico() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO;
    }

    public void finalizar() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser finalizada com diagnostico concluido");
        }
        status = StatusOrdemDeServico.FINALIZADA;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public String getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public StatusOrdemDeServico getStatus() {
        return status;
    }
}
