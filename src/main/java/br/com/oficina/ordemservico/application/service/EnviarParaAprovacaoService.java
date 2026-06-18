package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.EnviarParaAprovacaoCommand;
import br.com.oficina.ordemservico.application.usecase.EnviarParaAprovacaoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class EnviarParaAprovacaoService implements EnviarParaAprovacaoUseCase {
    private static final Logger log = LoggerFactory.getLogger(EnviarParaAprovacaoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public EnviarParaAprovacaoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void enviarParaAprovacao(EnviarParaAprovacaoCommand command) {
        log.info("Enviando ordem de servico para aprovacao. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServico.enviarParaAprovacao();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info("Ordem de servico enviada para aprovacao. numeroOrdemServico={}, situacao={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getSituacao().getDescricao());
    }
}
