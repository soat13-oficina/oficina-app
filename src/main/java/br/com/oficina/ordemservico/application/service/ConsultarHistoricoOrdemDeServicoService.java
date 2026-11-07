package br.com.oficina.ordemservico.application.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.ConsultarHistoricoOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.ConsultarHistoricoOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.HistoricoStatusOrdemDeServicoRepository;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
@Transactional(readOnly = true)
public class ConsultarHistoricoOrdemDeServicoService implements ConsultarHistoricoOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarHistoricoOrdemDeServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final HistoricoStatusOrdemDeServicoRepository historicoRepository;

    public ConsultarHistoricoOrdemDeServicoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            HistoricoStatusOrdemDeServicoRepository historicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.historicoRepository = historicoRepository;
    }

    @Override
    public HistoricoOrdemDeServico consultarHistorico(ConsultarHistoricoOrdemDeServicoQuery query) {
        log.info("Consultando historico de ordem de servico. numeroOrdemServico={}", query.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(query.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        List<AlteracaoStatusOrdemDeServico> alteracoes = historicoRepository.listarPorOrdem(ordemDeServico.getId());
        log.info("Consulta de historico de ordem de servico concluida. numeroOrdemServico={}, alteracoes={}",
                query.numeroOrdemServico(), alteracoes.size());
        return new HistoricoOrdemDeServico(ordemDeServico, alteracoes);
    }
}
