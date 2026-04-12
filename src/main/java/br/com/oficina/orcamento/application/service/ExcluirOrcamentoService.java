package br.com.oficina.orcamento.application.service;

import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new IllegalArgumentException("Orcamento nao encontrado"));
        orcamentoRepository.excluirPorId(command.orcamentoId());
    }
}
