package br.com.oficina.orcamento.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.application.command.ExcluirOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.ExcluirOrcamentoUseCase;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class ExcluirOrcamentoService implements ExcluirOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public ExcluirOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void excluirOrcamento(ExcluirOrcamentoCommand command) {
        orcamentoRepository.buscarPorId(command.orcamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));
        orcamentoRepository.excluirPorId(command.orcamentoId());
    }
}
