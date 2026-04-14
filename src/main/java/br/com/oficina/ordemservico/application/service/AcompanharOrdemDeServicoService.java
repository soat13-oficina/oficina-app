package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.AcompanharOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class AcompanharOrdemDeServicoService implements AcompanharOrdemDeServicoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public AcompanharOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public OrdemDeServico acompanhar(AcompanharOrdemDeServicoQuery query) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(query.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada"));

        if (ordemDeServico.getCliente().getCpfOuCnpj() == null
                || !ordemDeServico.getCliente().getCpfOuCnpj().equalsIgnoreCase(query.documentoCliente())) {
            throw new RecursoNaoEncontradoException("Ordem de servico nao encontrada");
        }

        return ordemDeServico;
    }
}
