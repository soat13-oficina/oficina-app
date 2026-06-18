package br.com.oficina.ordemservico.infrastructure.web;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.ConcluirServicoCommand;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.EntregarAoClienteCommand;
import br.com.oficina.ordemservico.application.command.EnviarParaAprovacaoCommand;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.IniciarExecucaoCommand;
import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.application.query.ConsultarStatusOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.AcompanharOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.AlterarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConcluirServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarOrdensDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarStatusOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase;
import br.com.oficina.ordemservico.application.usecase.CriarNovaOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.EntregarAoClienteUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarParaAprovacaoUseCase;
import br.com.oficina.ordemservico.application.usecase.ExcluirOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.application.usecase.IniciarExecucaoUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.AlterarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.AberturaOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.AcompanhamentoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.FinalizacaoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.OrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.StatusOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.TempoMedioExecucaoResponse;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemDeServicoController implements OrdemDeServicoControllerSwagger {
    private static final Logger log = LoggerFactory.getLogger(OrdemDeServicoController.class);

    private final AlterarOrdemDeServicoUseCase alterarOrdemDeServicoUseCase;
    private final CriarNovaOrdemDeServicoUseCase criarNovaOrdemDeServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    private final EntregarAoClienteUseCase entregarAoClienteUseCase;
    private final EnviarDiagnosticoParaOrcamentoUseCase enviarDiagnosticoParaOrcamentoUseCase;
    private final ExcluirOrdemDeServicoUseCase excluirOrdemDeServicoUseCase;
    private final FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase;
    private final ConsultarOrdensDeServicoUseCase consultarOrdensDeServicoUseCase;
    private final ConsultarTempoMedioExecucaoUseCase consultarTempoMedioExecucaoUseCase;
    private final AcompanharOrdemDeServicoUseCase acompanharOrdemDeServicoUseCase;
    private final EnviarParaAprovacaoUseCase enviarParaAprovacaoUseCase;
    private final IniciarExecucaoUseCase iniciarExecucaoUseCase;
    private final ConcluirServicoUseCase concluirServicoUseCase;
    private final ConsultarStatusOrdemDeServicoUseCase consultarStatusOrdemDeServicoUseCase;

    public OrdemDeServicoController(
            AlterarOrdemDeServicoUseCase alterarOrdemDeServicoUseCase,
            CriarNovaOrdemDeServicoUseCase criarNovaOrdemDeServicoUseCase,
            IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase,
            EntregarAoClienteUseCase entregarAoClienteUseCase,
            EnviarDiagnosticoParaOrcamentoUseCase enviarDiagnosticoParaOrcamentoUseCase,
            ExcluirOrdemDeServicoUseCase excluirOrdemDeServicoUseCase,
            FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase,
            ConsultarOrdensDeServicoUseCase consultarOrdensDeServicoUseCase,
            ConsultarTempoMedioExecucaoUseCase consultarTempoMedioExecucaoUseCase,
            AcompanharOrdemDeServicoUseCase acompanharOrdemDeServicoUseCase,
            EnviarParaAprovacaoUseCase enviarParaAprovacaoUseCase,
            IniciarExecucaoUseCase iniciarExecucaoUseCase,
            ConcluirServicoUseCase concluirServicoUseCase,
            ConsultarStatusOrdemDeServicoUseCase consultarStatusOrdemDeServicoUseCase) {
        this.alterarOrdemDeServicoUseCase = alterarOrdemDeServicoUseCase;
        this.criarNovaOrdemDeServicoUseCase = criarNovaOrdemDeServicoUseCase;
        this.iniciarDiagnosticoUseCase = iniciarDiagnosticoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
        this.entregarAoClienteUseCase = entregarAoClienteUseCase;
        this.enviarDiagnosticoParaOrcamentoUseCase = enviarDiagnosticoParaOrcamentoUseCase;
        this.excluirOrdemDeServicoUseCase = excluirOrdemDeServicoUseCase;
        this.finalizarOrdemDeServicoUseCase = finalizarOrdemDeServicoUseCase;
        this.consultarOrdensDeServicoUseCase = consultarOrdensDeServicoUseCase;
        this.consultarTempoMedioExecucaoUseCase = consultarTempoMedioExecucaoUseCase;
        this.acompanharOrdemDeServicoUseCase = acompanharOrdemDeServicoUseCase;
        this.enviarParaAprovacaoUseCase = enviarParaAprovacaoUseCase;
        this.iniciarExecucaoUseCase = iniciarExecucaoUseCase;
        this.concluirServicoUseCase = concluirServicoUseCase;
        this.consultarStatusOrdemDeServicoUseCase = consultarStatusOrdemDeServicoUseCase;
    }

    @PostMapping
    public ResponseEntity<AberturaOrdemDeServicoResponse> criar(@RequestBody CriarOrdemDeServicoRequest request) {
        log.info("Recebida requisicao de criacao de ordem de servico. clienteId={}, funcionarioId={}, placaVeiculo={}",
                request.clienteId(),
                request.funcionarioId(),
                request.placaVeiculo());
        String numeroOrdemServico = criarNovaOrdemDeServicoUseCase.criarNovaOrdemDeServico(toCommand(request));
        log.info("Requisicao de criacao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{numeroOrdemServico}")
                .buildAndExpand(numeroOrdemServico)
                .toUri();
        return ResponseEntity.created(location)
                .body(new AberturaOrdemDeServicoResponse(numeroOrdemServico, "Recebida"));
    }

    private static CriarOrdemDeServicoCommand toCommand(CriarOrdemDeServicoRequest request) {
        List<CriarOrdemDeServicoCommand.ServicoItem> servicos = request.servicos() == null
                ? List.of()
                : request.servicos().stream()
                        .map(servico -> new CriarOrdemDeServicoCommand.ServicoItem(servico.descricao(), servico.valorMaoDeObra()))
                        .toList();
        List<CriarOrdemDeServicoCommand.PecaItem> pecas = request.pecasPrevistas() == null
                ? List.of()
                : request.pecasPrevistas().stream()
                        .map(peca -> new CriarOrdemDeServicoCommand.PecaItem(peca.pecaInsumoId(), peca.quantidade()))
                        .toList();
        return new CriarOrdemDeServicoCommand(
                request.clienteId(), request.funcionarioId(), request.placaVeiculo(), servicos, pecas);
    }

    @PutMapping("/{numeroOrdemServico}")
    public ResponseEntity<Void> alterar(
            @PathVariable String numeroOrdemServico,
            @RequestBody AlterarOrdemDeServicoRequest request) {
        log.info(
                "Recebida requisicao de alteracao de ordem de servico. numeroOrdemServico={}, clienteId={}, funcionarioId={}, placaVeiculo={}",
                numeroOrdemServico,
                request.clienteId(),
                request.funcionarioId(),
                request.placaVeiculo());
        alterarOrdemDeServicoUseCase.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand(
                numeroOrdemServico,
                request.clienteId(),
                request.funcionarioId(),
                request.placaVeiculo()));
        log.info("Requisicao de alteracao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{numeroOrdemServico}")
    public ResponseEntity<Void> excluir(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de exclusao de ordem de servico. numeroOrdemServico={}", numeroOrdemServico);
        excluirOrdemDeServicoUseCase.excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand(numeroOrdemServico));
        log.info("Requisicao de exclusao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<OrdemDeServicoResponse>> consultar(
            @RequestParam(required = false) String numeroOrdemServico,
            @RequestParam(required = false) String nomeCliente,
            @RequestParam(required = false) String placaVeiculo,
            @RequestParam(required = false) String documentoCliente) {
        log.info(
                "Recebida requisicao de consulta de ordens de servico. numeroOrdemServico={}, nomeCliente={}, placaVeiculo={}, documentoClienteInformado={}",
                numeroOrdemServico,
                nomeCliente,
                placaVeiculo,
                documentoCliente != null);
        List<OrdemDeServicoResponse> response = consultarOrdensDeServicoUseCase.consultarOrdensDeServico(
                        new ConsultarOrdensDeServicoQuery(
                                numeroOrdemServico,
                                nomeCliente,
                                placaVeiculo,
                                documentoCliente))
                .stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
        log.info("Requisicao de consulta de ordens de servico concluida. quantidadeOrdensDeServico={}", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metricas/tempo-medio")
    public ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao() {
        log.info("Recebida requisicao de consulta da metrica de tempo medio de execucao.");
        TempoMedioExecucaoResponse response = TempoMedioExecucaoResponse.from(
                consultarTempoMedioExecucaoUseCase.consultarTempoMedioExecucao());
        log.info("Requisicao de consulta da metrica de tempo medio concluida. quantidadeOrdensConsideradas={}",
                response.quantidadeOrdensConsideradas());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{numeroOrdemServico}/acompanhamento")
    public ResponseEntity<AcompanhamentoOrdemDeServicoResponse> acompanhar(
            @PathVariable String numeroOrdemServico,
            @RequestParam String documentoCliente) {
        log.info(
                "Recebida requisicao de acompanhamento de ordem de servico. numeroOrdemServico={}, documentoClienteInformado={}",
                numeroOrdemServico,
                documentoCliente != null);
        AcompanhamentoOrdemDeServicoResponse response = AcompanhamentoOrdemDeServicoResponse.from(
                acompanharOrdemDeServicoUseCase.acompanhar(
                        new AcompanharOrdemDeServicoQuery(numeroOrdemServico, documentoCliente)));
        log.info("Requisicao de acompanhamento de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/iniciar")
    public ResponseEntity<Void> iniciarDiagnostico(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para iniciar diagnostico. numeroOrdemServico={}", numeroOrdemServico);
        iniciarDiagnosticoUseCase.iniciarDiagnostico(new IniciarDiagnosticoCommand(numeroOrdemServico));
        log.info("Requisicao para iniciar diagnostico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/concluir")
    public ResponseEntity<Void> concluirDiagnostico(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para concluir diagnostico. numeroOrdemServico={}", numeroOrdemServico);
        concluirDiagnosticoUseCase.concluirDiagnostico(new ConcluirDiagnosticoCommand(numeroOrdemServico));
        log.info("Requisicao para concluir diagnostico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{numeroOrdemServico}")
    public ResponseEntity<StatusOrdemDeServicoResponse> consultarStatus(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de consulta de status. numeroOrdemServico={}", numeroOrdemServico);
        StatusOrdemDeServicoResponse response = StatusOrdemDeServicoResponse.from(
                consultarStatusOrdemDeServicoUseCase.consultarStatus(
                        new ConsultarStatusOrdemDeServicoQuery(numeroOrdemServico)));
        log.info("Requisicao de consulta de status concluida. numeroOrdemServico={}, situacao={}",
                numeroOrdemServico, response.situacao());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroOrdemServico}/orcamento/enviar-aprovacao")
    public ResponseEntity<Void> enviarParaAprovacao(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para enviar ordem de servico para aprovacao. numeroOrdemServico={}", numeroOrdemServico);
        enviarParaAprovacaoUseCase.enviarParaAprovacao(new EnviarParaAprovacaoCommand(numeroOrdemServico));
        log.info("Requisicao para enviar para aprovacao concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/execucao/iniciar")
    public ResponseEntity<Void> iniciarExecucao(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para iniciar execucao. numeroOrdemServico={}", numeroOrdemServico);
        iniciarExecucaoUseCase.iniciarExecucao(new IniciarExecucaoCommand(numeroOrdemServico));
        log.info("Requisicao para iniciar execucao concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/servico/concluir")
    public ResponseEntity<Void> concluirServico(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para concluir servico. numeroOrdemServico={}", numeroOrdemServico);
        concluirServicoUseCase.concluirServico(new ConcluirServicoCommand(numeroOrdemServico));
        log.info("Requisicao para concluir servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/enviar-para-orcamento")
    public ResponseEntity<Void> enviarDiagnosticoParaOrcamento(
            @PathVariable String numeroOrdemServico,
            @RequestBody EnviarDiagnosticoParaOrcamentoRequest request) {
        log.info(
                "Recebida requisicao para enviar diagnostico para orcamento. numeroOrdemServico={}, quantidadeServicosPropostos={}, quantidadePecasPrevistas={}",
                numeroOrdemServico,
                request.servicosPropostos() == null ? 0 : request.servicosPropostos().size(),
                request.pecasPrevistas() == null ? 0 : request.pecasPrevistas().size());
        enviarDiagnosticoParaOrcamentoUseCase.enviarDiagnosticoParaOrcamento(
                new EnviarDiagnosticoParaOrcamentoUseCase.EnviarDiagnosticoParaOrcamentoRequest(
                        numeroOrdemServico,
                        request.descricaoDiagnostico(),
                        request.servicosPropostos(),
                        request.toPecasInput(),
                        request.valorMaoDeObra(),
                        request.desconto(),
                        request.validade(),
                        request.observacoes()));
        log.info("Requisicao para enviar diagnostico para orcamento concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/finalizacao")
    public ResponseEntity<FinalizacaoOrdemDeServicoResponse> finalizar(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de finalizacao de ordem de servico. numeroOrdemServico={}", numeroOrdemServico);
        FinalizacaoOrdemDeServicoResponse response = FinalizacaoOrdemDeServicoResponse.from(
                finalizarOrdemDeServicoUseCase.finalizarOrdemDeServico(new FinalizarOrdemDeServicoCommand(numeroOrdemServico)));
        log.info("Requisicao de finalizacao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroOrdemServico}/entrega")
    public ResponseEntity<Void> entregarAoCliente(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de entrega ao cliente. numeroOrdemServico={}", numeroOrdemServico);
        entregarAoClienteUseCase.entregarAoCliente(new EntregarAoClienteCommand(numeroOrdemServico));
        log.info("Requisicao de entrega ao cliente concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }
}
