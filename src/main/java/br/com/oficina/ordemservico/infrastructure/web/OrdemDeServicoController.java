package br.com.oficina.ordemservico.infrastructure.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.EntregarAoClienteCommand;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.AcompanharOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.AlterarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarOrdensDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase;
import br.com.oficina.ordemservico.application.usecase.CriarNovaOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.EntregarAoClienteUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.ExcluirOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.AlterarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.AcompanhamentoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.FinalizacaoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.OrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.TempoMedioExecucaoResponse;

@RestController
@RequestMapping("/ordens-servico")
@Tag(name = "Ordens de Servico", description = "Operações de abertura, acompanhamento, execução, entrega e métricas de ordens de serviço")
@SecurityRequirement(name = "bearerAuth")
public class OrdemDeServicoController {
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
            AcompanharOrdemDeServicoUseCase acompanharOrdemDeServicoUseCase) {
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
    }

    @PostMapping
    @Operation(
            summary = "Criar ordem de serviço",
            description = "Abre uma nova ordem de serviço vinculando cliente, funcionário responsável e veículo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Ordem de serviço criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido ou dados inconsistentes", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente, funcionário ou veículo não encontrado", content = @Content)
    })
    public ResponseEntity<Void> criar(@RequestBody CriarOrdemDeServicoRequest request) {
        log.info("Recebida requisicao de criacao de ordem de servico. clienteId={}, funcionarioId={}, placaVeiculo={}",
                request.clienteId(),
                request.funcionarioId(),
                request.placaVeiculo());
        criarNovaOrdemDeServicoUseCase.criarNovaOrdemDeServico(
                new CriarOrdemDeServicoCommand(request.clienteId(), request.funcionarioId(), request.placaVeiculo()));
        log.info("Requisicao de criacao de ordem de servico concluida. clienteId={}, funcionarioId={}, placaVeiculo={}",
                request.clienteId(),
                request.funcionarioId(),
                request.placaVeiculo());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{numeroOrdemServico}")
    @Operation(
            summary = "Alterar ordem de serviço",
            description = "Atualiza cliente e veículo de uma ordem aberta, mantendo o funcionário criador original.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido ou regra de negócio violada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem, cliente, funcionário ou veículo não encontrado", content = @Content)
    })
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
    @Operation(summary = "Excluir ordem de serviço", description = "Remove uma ordem de serviço pelo número informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de exclusao de ordem de servico. numeroOrdemServico={}", numeroOrdemServico);
        excluirOrdemDeServicoUseCase.excluirOrdemDeServico(new ExcluirOrdemDeServicoCommand(numeroOrdemServico));
        log.info("Requisicao de exclusao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Consultar ordens de serviço",
            description = "Consulta ordens de serviço por número, nome do cliente, documento do cliente e/ou placa do veículo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordens de serviço retornadas com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrdemDeServicoResponse.class))))
    })
    public ResponseEntity<List<OrdemDeServicoResponse>> consultar(
            @Parameter(description = "Número da ordem de serviço para filtro exato.") @RequestParam(required = false) String numeroOrdemServico,
            @Parameter(description = "Nome completo ou parcial do cliente vinculado.") @RequestParam(required = false) String nomeCliente,
            @Parameter(description = "Placa do veículo vinculada à ordem de serviço.") @RequestParam(required = false) String placaVeiculo,
            @Parameter(description = "CPF ou CNPJ do cliente vinculado.") @RequestParam(required = false) String documentoCliente) {
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
    @Operation(
            summary = "Consultar tempo médio de execução",
            description = "Retorna a média geral de execução calculada a partir de iniciadaEm e finalizadaEm das ordens com execução concluída, incluindo ordens já entregues ao cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métrica retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = TempoMedioExecucaoResponse.class)))
    })
    public ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao() {
        log.info("Recebida requisicao de consulta da metrica de tempo medio de execucao.");
        TempoMedioExecucaoResponse response = TempoMedioExecucaoResponse.from(
                consultarTempoMedioExecucaoUseCase.consultarTempoMedioExecucao());
        log.info("Requisicao de consulta da metrica de tempo medio concluida. quantidadeOrdensConsideradas={}",
                response.quantidadeOrdensConsideradas());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{numeroOrdemServico}/acompanhamento")
    @Operation(
            summary = "Acompanhar ordem de serviço",
            description = "Permite ao cliente consultar o andamento da ordem de serviço informando o número da ordem e o documento do cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acompanhamento retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = AcompanhamentoOrdemDeServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    public ResponseEntity<AcompanhamentoOrdemDeServicoResponse> acompanhar(
            @PathVariable String numeroOrdemServico,
            @Parameter(description = "CPF ou CNPJ do cliente vinculado à ordem de serviço.") @RequestParam String documentoCliente) {
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
    @Operation(summary = "Iniciar diagnóstico", description = "Inicia o diagnóstico de uma ordem de serviço aberta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diagnóstico iniciado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    public ResponseEntity<Void> iniciarDiagnostico(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para iniciar diagnostico. numeroOrdemServico={}", numeroOrdemServico);
        iniciarDiagnosticoUseCase.iniciarDiagnostico(new IniciarDiagnosticoCommand(numeroOrdemServico));
        log.info("Requisicao para iniciar diagnostico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/concluir")
    @Operation(summary = "Concluir diagnóstico", description = "Conclui o diagnóstico de uma ordem de serviço em andamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diagnóstico concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    public ResponseEntity<Void> concluirDiagnostico(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao para concluir diagnostico. numeroOrdemServico={}", numeroOrdemServico);
        concluirDiagnosticoUseCase.concluirDiagnostico(new ConcluirDiagnosticoCommand(numeroOrdemServico));
        log.info("Requisicao para concluir diagnostico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/enviar-para-orcamento")
    @Operation(
            summary = "Enviar diagnóstico para orçamento",
            description = "Gera um orçamento a partir do diagnóstico concluído e move a ordem de serviço para o status de orçamento gerado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diagnóstico enviado para orçamento com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
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
    @Operation(summary = "Finalizar ordem de serviço", description = "Finaliza uma ordem de serviço com orçamento já gerado, registrando o timestamp de finalização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço finalizada com sucesso",
                    content = @Content(schema = @Schema(implementation = FinalizacaoOrdemDeServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço ou orçamento não encontrado", content = @Content)
    })
    public ResponseEntity<FinalizacaoOrdemDeServicoResponse> finalizar(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de finalizacao de ordem de servico. numeroOrdemServico={}", numeroOrdemServico);
        FinalizacaoOrdemDeServicoResponse response = FinalizacaoOrdemDeServicoResponse.from(
                finalizarOrdemDeServicoUseCase.finalizarOrdemDeServico(new FinalizarOrdemDeServicoCommand(numeroOrdemServico)));
        log.info("Requisicao de finalizacao de ordem de servico concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroOrdemServico}/entrega")
    @Operation(summary = "Entregar ordem de serviço", description = "Conclui a entrega de uma ordem de serviço finalizada ao cliente, alterando o status para ENTREGUE e registrando o timestamp de entrega.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço entregue com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    public ResponseEntity<Void> entregarAoCliente(@PathVariable String numeroOrdemServico) {
        log.info("Recebida requisicao de entrega ao cliente. numeroOrdemServico={}", numeroOrdemServico);
        entregarAoClienteUseCase.entregarAoCliente(new EntregarAoClienteCommand(numeroOrdemServico));
        log.info("Requisicao de entrega ao cliente concluida. numeroOrdemServico={}", numeroOrdemServico);
        return ResponseEntity.noContent().build();
    }
}
