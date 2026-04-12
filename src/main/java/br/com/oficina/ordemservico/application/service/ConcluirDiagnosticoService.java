package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirDiagnosticoService implements ConcluirDiagnosticoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConcluirDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void concluirDiagnostico(ConcluirDiagnosticoCommand command) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));
        ordemDeServico.concluirDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
