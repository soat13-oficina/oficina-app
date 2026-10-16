package br.com.oficina.ordemservico.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.application.PassosDaSaga;

/**
 * Envia o diagnostico concluido para orcamento. Fase 4: o OS nao cria mais o orcamento - a Saga manda
 * o comando {@code GerarOrcamento} ao Billing e a OS vai para "Aguardando Aprovacao" quando o
 * {@code OrcamentoGerado} chegar (ADR 0004, passo 2). Aqui so o que da para validar sem o Billing.
 */
@Service
public class EnviarDiagnosticoParaOrcamentoService implements EnviarDiagnosticoParaOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(EnviarDiagnosticoParaOrcamentoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final PassosDaSaga passosDaSaga;

    public EnviarDiagnosticoParaOrcamentoService(OrdemDeServicoRepository ordemDeServicoRepository, PassosDaSaga passosDaSaga) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.passosDaSaga = passosDaSaga;
    }

    @Override
    @Transactional
    public void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request) {
        log.info("Iniciando envio de diagnostico para orcamento. numeroOrdemServico={}", request.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(request.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));

        if (ordemDeServico.getStatus() != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser enviada para orcamento com diagnostico concluido");
        }
        // O diagnostico deve ter sido fechado com peças/descrição (fonte do orçamento).
        if (ordemDeServico.getServicos().isEmpty() && ordemDeServico.getPecasPrevistas().isEmpty()) {
            throw new RegraDeNegocioException(
                    "Diagnostico nao fechado: registre as pecas e a descricao do servico no fechamento do diagnostico.");
        }
        validarDadosFinanceiros(request);

        passosDaSaga.solicitarOrcamento(ordemDeServico.getId(), new PassosDaSaga.DadosOrcamento(
                request.valorMaoDeObra(), request.desconto(), request.validade(), request.observacoes()));
        log.info("Diagnostico enviado para orcamento; aguardando o Billing. numeroOrdemServico={}",
                ordemDeServico.getNumeroOrdemServico());
    }

    /** Erros de digitacao respondem 400 agora, em vez de virarem OrcamentoFalhou depois. */
    private static void validarDadosFinanceiros(EnviarDiagnosticoParaOrcamentoRequest request) {
        if (request.valorMaoDeObra() == null || request.valorMaoDeObra().signum() < 0) {
            throw new RegraDeNegocioException("Valor da mao de obra e obrigatorio e nao pode ser negativo.");
        }
        if (request.desconto() != null && request.desconto().compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Desconto nao pode ser negativo.");
        }
        if (request.validade() != null && !request.validade().isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Validade do orcamento deve ser uma data futura.");
        }
    }
}
