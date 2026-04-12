package br.com.oficina.application.orcamento;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.oficina.domain.model.orcamento.Orcamento;
import br.com.oficina.domain.repository.OrcamentoRepository;

@Service
public class ConsultarOrcamentoService implements ConsultarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public ConsultarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public Optional<Orcamento> consultarOrcamento(ConsultarOrcamentoRequest request) {
        return orcamentoRepository.buscarPorId(request.orcamentoId());
    }
}
