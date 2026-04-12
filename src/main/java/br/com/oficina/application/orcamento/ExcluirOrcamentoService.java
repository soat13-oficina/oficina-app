package br.com.oficina.application.orcamento;

import org.springframework.stereotype.Service;

import br.com.oficina.domain.repository.OrcamentoRepository;

@Service
public class ExcluirOrcamentoService implements ExcluirOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public ExcluirOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void excluirOrcamento(ExcluirOrcamentoRequest request) {
        orcamentoRepository.excluirPorId(request.orcamentoId());
    }
}
