package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.AguardarAprovacaoCommand;
import br.com.oficina.ordemservico.application.usecase.AguardarAprovacaoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class AguardarAprovacaoService implements AguardarAprovacaoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AguardarAprovacaoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public AguardarAprovacaoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void aguardarAprovacao(AguardarAprovacaoCommand command) {
        log.info("Enviando ordem de servico para aprovacao do cliente. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServico.aguardarAprovacao();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info("Ordem de servico aguardando aprovacao. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }
}
