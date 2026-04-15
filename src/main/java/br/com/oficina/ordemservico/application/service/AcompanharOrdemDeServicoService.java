package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.AcompanharOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class AcompanharOrdemDeServicoService implements AcompanharOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AcompanharOrdemDeServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public AcompanharOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public OrdemDeServico acompanhar(AcompanharOrdemDeServicoQuery query) {
        log.info("Iniciando acompanhamento de ordem de servico. numeroOrdemServico={}, documentoClienteInformado={}",
                query.numeroOrdemServico(),
                query.documentoCliente() != null);
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(query.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada"));

        if (ordemDeServico.getCliente().getCpfOuCnpj() == null
                || !ordemDeServico.getCliente().getCpfOuCnpj().equalsIgnoreCase(query.documentoCliente())) {
            throw new RecursoNaoEncontradoException("Ordem de servico nao encontrada");
        }

        log.info("Acompanhamento de ordem de servico concluido. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
        return ordemDeServico;
    }
}
