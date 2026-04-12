package br.com.oficina.application.orcamento;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.domain.model.orcamento.Orcamento;
import br.com.oficina.domain.repository.OrcamentoRepository;

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
