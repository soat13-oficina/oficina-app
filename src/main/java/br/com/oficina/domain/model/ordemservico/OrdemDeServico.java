package br.com.oficina.domain.model.ordemservico;

import br.com.oficina.domain.model.cliente.Cliente;
import br.com.oficina.domain.model.veiculo.Veiculo;
import br.com.oficina.domain.exception.RegraDeNegocioException;

public class OrdemDeServico {
    private final String id;
    private final Funcionario funcionario;
    private final Cliente cliente;
    private final Veiculo veiculo;
    private StatusOrdemDeServico status = StatusOrdemDeServico.ABERTA;

    private OrdemDeServico(String id, Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        this.id = id;
        this.funcionario = funcionario;
        this.cliente = cliente;
        this.veiculo = veiculo;
    }

    public static OrdemDeServico abrir(String id, Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        return new OrdemDeServico(id, funcionario, cliente, veiculo);
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

    public String getId() {
        return id;
    }
}
