package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.IniciarExecucaoCommand;
import br.com.oficina.ordemservico.application.usecase.IniciarExecucaoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class IniciarExecucaoService implements IniciarExecucaoUseCase {
    private static final Logger log = LoggerFactory.getLogger(IniciarExecucaoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public IniciarExecucaoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void iniciarExecucao(IniciarExecucaoCommand command) {
        log.info("Iniciando execucao do servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServico.iniciarExecucao();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info("Execucao do servico iniciada com sucesso. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }
}
