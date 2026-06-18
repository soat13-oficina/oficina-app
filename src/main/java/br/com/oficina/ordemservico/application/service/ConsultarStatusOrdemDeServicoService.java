package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.ConsultarStatusOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.ConsultarStatusOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
@Transactional(readOnly = true)
public class ConsultarStatusOrdemDeServicoService implements ConsultarStatusOrdemDeServicoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConsultarStatusOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public OrdemDeServico consultarStatus(ConsultarStatusOrdemDeServicoQuery query) {
        return ordemDeServicoRepository.buscarPorNumero(query.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
    }
}
