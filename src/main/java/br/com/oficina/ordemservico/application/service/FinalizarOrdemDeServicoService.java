package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class FinalizarOrdemDeServicoService implements FinalizarOrdemDeServicoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public FinalizarOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void finalizarOrdemDeServico(FinalizarOrdemDeServicoCommand command) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorId(command.ordemDeServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));
        ordemDeServico.finalizar();
        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
