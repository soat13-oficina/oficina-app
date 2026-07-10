package br.com.oficina.cliente.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.oficina.cliente.domain.repository.VerificadorVinculosCliente;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/**
 * Adaptador da porta {@link VerificadorVinculosCliente}: consulta a existência de veículos ou ordens de
 * serviço vinculados ao cliente via os repositórios Spring Data de `veiculo` e `ordemservico`. A coligação
 * fica em infra→infra, preservando a arquitetura (sem dependência circular de domínio).
 */
@Component
public class VerificadorVinculosClienteAdapter implements VerificadorVinculosCliente {
    private final SpringDataVeiculoRepository veiculoRepository;
    private final SpringDataOrdemDeServicoRepository ordemDeServicoRepository;

    public VerificadorVinculosClienteAdapter(
            SpringDataVeiculoRepository veiculoRepository,
            SpringDataOrdemDeServicoRepository ordemDeServicoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public boolean possuiVinculos(UUID clienteId) {
        return veiculoRepository.existsByClienteId(clienteId)
                || ordemDeServicoRepository.existsByClienteId(clienteId);
    }
}
