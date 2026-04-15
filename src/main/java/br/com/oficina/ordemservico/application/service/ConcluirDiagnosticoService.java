package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirDiagnosticoService implements ConcluirDiagnosticoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConcluirDiagnosticoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConcluirDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void concluirDiagnostico(ConcluirDiagnosticoCommand command) {
        log.info("Concluindo diagnostico de ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServico.concluirDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info("Diagnostico concluido com sucesso. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }
}
