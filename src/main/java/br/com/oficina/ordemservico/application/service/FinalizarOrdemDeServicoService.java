package br.com.oficina.ordemservico.application.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.FinalizacaoOrdemDeServico;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.ClienteFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.PecaFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase.VeiculoFinalizacao;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.pecainsumo.application.command.ConsumirPecaCommand;
import br.com.oficina.pecainsumo.application.usecase.ConsumirPecaUseCase;

@Service
public class FinalizarOrdemDeServicoService implements FinalizarOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(FinalizarOrdemDeServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ConsumirPecaUseCase consumirPecaUseCase;

    public FinalizarOrdemDeServicoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            OrcamentoRepository orcamentoRepository,
            ConsumirPecaUseCase consumirPecaUseCase) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.consumirPecaUseCase = consumirPecaUseCase;
    }

    @Override
    public FinalizacaoOrdemDeServico finalizarOrdemDeServico(FinalizarOrdemDeServicoCommand command) {
        log.info("Iniciando finalizacao de ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        Orcamento orcamento = orcamentoRepository.buscarPorOrdemDeServicoId(
                        ordemDeServico.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para a ordem de servico informada."));

        // Consumir peças do estoque (baixa definitiva)
        for (PecaOrcamento peca : orcamento.getPecasOrcamento()) {
            consumirPecaUseCase.consumirPeca(
                    new ConsumirPecaCommand(peca.getPecaInsumoId(), peca.getQuantidade()));
            log.debug("Peca consumida do estoque. pecaInsumoId={}, quantidade={}", peca.getPecaInsumoId(), peca.getQuantidade());
        }

        ordemDeServico.finalizar();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info(
                "Ordem de servico finalizada com sucesso. numeroOrdemServico={}, statusAtual={}, valorTotalOrcamento={}, pecasConsumidas={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus(),
                orcamento.getValorTotal(),
                orcamento.getPecasOrcamento().size());
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
                        .map(peca -> new PecaFinalizacao(peca.getPecaInsumoId(), peca.getDescricao(), peca.getPreco(), peca.getQuantidade()))
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
