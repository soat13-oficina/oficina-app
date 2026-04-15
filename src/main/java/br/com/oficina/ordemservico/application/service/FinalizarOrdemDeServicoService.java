package br.com.oficina.ordemservico.application.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.FinalizacaoOrdemDeServico;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.ClienteFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.PecaFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.VeiculoFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class FinalizarOrdemDeServicoService implements FinalizarOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(FinalizarOrdemDeServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final OrcamentoRepository orcamentoRepository;

    public FinalizarOrdemDeServicoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            OrcamentoRepository orcamentoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public FinalizacaoOrdemDeServico finalizarOrdemDeServico(FinalizarOrdemDeServicoCommand command) {
        log.info("Iniciando finalizacao de ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        Orcamento orcamento = orcamentoRepository.buscarPorOrdemDeServicoId(
                        ordemDeServico.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para a ordem de servico informada."));
        ordemDeServico.finalizar();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info(
                "Ordem de servico finalizada com sucesso. numeroOrdemServico={}, statusAtual={}, valorTotalOrcamento={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus(),
                orcamento.getValorTotal());
        return new FinalizacaoOrdemDeServico(
                ordemDeServico.getNumeroOrdemServico(),
                new ClienteFinalizacao(
                        ordemDeServico.getCliente().getId().toString(),
                        ordemDeServico.getCliente().getNome(),
                        ordemDeServico.getCliente().getCpfOuCnpj()),
                new VeiculoFinalizacao(
                        ordemDeServico.getVeiculoId().toString(),
                        ordemDeServico.getVeiculo().getPlaca(),
                        ordemDeServico.getVeiculo().getMarca(),
                        ordemDeServico.getVeiculo().getModelo()),
                formatarTempoExecucao(ordemDeServico),
                orcamento.getServicosPropostos().isEmpty()
                        ? orcamento.getDescricaoDiagnostico()
                        : String.join(", ", orcamento.getServicosPropostos()),
                orcamento.getValorMaoDeObra(),
                orcamento.getPecasOrcamento().stream()
                        .map(peca -> new PecaFinalizacao(peca.getDescricao(), peca.getPreco()))
                        .toList(),
                orcamento.getValorTotal(),
                orcamento.getDesconto());
    }

    private String formatarTempoExecucao(OrdemDeServico ordemDeServico) {
        Duration duracao = Duration.between(ordemDeServico.getIniciadaEm(), ordemDeServico.getFinalizadaEm());
        long totalHoras = duracao.toHours();
        if (totalHoras < 24) {
            return totalHoras + " hora" + (totalHoras == 1 ? "" : "s");
        }
        long dias = totalHoras / 24;
        long horasRestantes = totalHoras % 24;
        if (horasRestantes == 0) {
            return dias + " dia" + (dias == 1 ? "" : "s");
        }
        return dias + " dia" + (dias == 1 ? "" : "s") + " e "
                + horasRestantes + " hora" + (horasRestantes == 1 ? "" : "s");
    }
}
