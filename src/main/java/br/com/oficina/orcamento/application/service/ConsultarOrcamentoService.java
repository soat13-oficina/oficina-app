package br.com.oficina.orcamento.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.application.usecase.ConsultarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class ConsultarOrcamentoService implements ConsultarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public ConsultarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public Optional<Orcamento> consultarOrcamento(ConsultarOrcamentoQuery query) {
        return orcamentoRepository.buscarPorId(query.orcamentoId());
    }
}
