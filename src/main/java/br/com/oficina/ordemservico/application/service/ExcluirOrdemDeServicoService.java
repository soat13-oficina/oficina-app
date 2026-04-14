package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.ExcluirOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ExcluirOrdemDeServicoService implements ExcluirOrdemDeServicoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ExcluirOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void excluirOrdemDeServico(ExcluirOrdemDeServicoCommand command) {
        ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServicoRepository.excluirPorNumero(command.numeroOrdemServico());
    }
}
