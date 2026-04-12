package br.com.oficina.orcamento.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.usecase.ListarOrcamentosUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class ListarOrcamentosService implements ListarOrcamentosUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public ListarOrcamentosService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public List<Orcamento> listarOrcamentos() {
        return orcamentoRepository.buscarTodos();
    }
}
