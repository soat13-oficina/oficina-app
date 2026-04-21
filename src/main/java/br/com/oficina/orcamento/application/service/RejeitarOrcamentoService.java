package br.com.oficina.orcamento.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.RejeitarOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.RejeitarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class RejeitarOrcamentoService implements RejeitarOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(RejeitarOrcamentoService.class);

    private final OrcamentoRepository orcamentoRepository;

    public RejeitarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void rejeitarOrcamento(RejeitarOrcamentoCommand command) {
        log.info("Iniciando rejeicao de orcamento. numeroOrcamento={}", command.numeroOrcamento());
        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));

        if (orcamento.getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode ser rejeitado quando estiver aguardando aprovacao.");
        }

        orcamento.rejeitar();
        orcamentoRepository.atualizar(orcamento);
        log.info("Orcamento rejeitado com sucesso. numeroOrcamento={}", command.numeroOrcamento());
    }
}
