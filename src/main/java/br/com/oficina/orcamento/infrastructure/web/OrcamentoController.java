package br.com.oficina.orcamento.infrastructure.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.command.ExcluirOrcamentoCommand;
import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.application.usecase.AlterarOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.ConsultarOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.ExcluirOrcamentoUseCase;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.infrastructure.web.request.AlterarOrcamentoRequest;
import br.com.oficina.orcamento.infrastructure.web.request.CadastrarOrcamentoRequest;
import br.com.oficina.orcamento.infrastructure.web.response.OrcamentoResponse;

@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orcamentos", description = "Operações de cadastro, consulta, alteração e exclusão de orçamentos")
@SecurityRequirement(name = "bearerAuth")
public class OrcamentoController {
    private static final Logger log = LoggerFactory.getLogger(OrcamentoController.class);

    private final CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase;
    private final ConsultarOrcamentoUseCase consultarOrcamentoUseCase;
    private final AlterarOrcamentoUseCase alterarOrcamentoUseCase;
    private final ExcluirOrcamentoUseCase excluirOrcamentoUseCase;

    public OrcamentoController(
            CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase,
            ConsultarOrcamentoUseCase consultarOrcamentoUseCase,
            AlterarOrcamentoUseCase alterarOrcamentoUseCase,
            ExcluirOrcamentoUseCase excluirOrcamentoUseCase) {
        this.cadastrarNovoOrcamentoUseCase = cadastrarNovoOrcamentoUseCase;
        this.consultarOrcamentoUseCase = consultarOrcamentoUseCase;
        this.alterarOrcamentoUseCase = alterarOrcamentoUseCase;
        this.excluirOrcamentoUseCase = excluirOrcamentoUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar orçamento",
            description = "Cria um novo orçamento vinculado a um cliente existente. O número do orçamento deve ser único.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orçamento cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarOrcamentoRequest request) {
        log.info("Recebida requisicao de cadastro de orcamento. numeroOrcamento={}, clienteId={}, ordemDeServicoId={}",
                request.numeroOrcamento(), request.clienteId(), request.ordemDeServicoId());
        cadastrarNovoOrcamentoUseCase.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                request.numeroOrcamento(),
                validarClienteId(request.clienteId()),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.placaVeiculo(),
                request.marcaVeiculo(),
                request.modeloVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                request.validade(),
                request.observacoes()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.numeroOrcamento())
                .toUri();
        log.info("Requisicao de cadastro de orcamento concluida. numeroOrcamento={}", request.numeroOrcamento());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{orcamentoId}")
    @Operation(summary = "Consultar orçamento", description = "Consulta um orçamento pelo número do orçamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento encontrado",
                    content = @Content(schema = @Schema(implementation = OrcamentoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content)
    })
    public ResponseEntity<OrcamentoResponse> consultar(@PathVariable String orcamentoId) {
        log.info("Recebida requisicao de consulta de orcamento. numeroOrcamento={}", orcamentoId);
        OrcamentoResponse response = consultarOrcamentoUseCase
                .consultarOrcamento(new ConsultarOrcamentoQuery(orcamentoId, null, null))
                .stream()
                .findFirst()
                .map(OrcamentoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));
        log.info("Requisicao de consulta de orcamento concluida. numeroOrcamento={}", orcamentoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Consultar orçamentos",
            description = "Consulta orçamentos por número do orçamento, CPF do cliente e/ou placa do veículo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamentos retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrcamentoResponse.class))))
    })
    public ResponseEntity<List<OrcamentoResponse>> consultarPorFiltros(
            @Parameter(description = "Número do orçamento para filtro exato.") @RequestParam(required = false) String numeroOrcamento,
            @Parameter(description = "CPF do cliente vinculado ao orçamento.") @RequestParam(required = false) String cpfCliente,
            @Parameter(description = "Placa do veículo vinculada ao orçamento.") @RequestParam(required = false) String placaVeiculo) {
        log.info("Recebida requisicao de consulta de orcamentos. numeroInformado={}, cpfInformado={}, placaInformada={}",
                numeroOrcamento != null && !numeroOrcamento.isBlank(),
                cpfCliente != null && !cpfCliente.isBlank(),
                placaVeiculo != null && !placaVeiculo.isBlank());
        List<OrcamentoResponse> orcamentos = consultarOrcamentoUseCase
                .consultarOrcamento(new ConsultarOrcamentoQuery(numeroOrcamento, cpfCliente, placaVeiculo))
                .stream()
                .map(OrcamentoResponse::from)
                .toList();
        log.info("Requisicao de consulta de orcamentos concluida. quantidadeOrcamentos={}", orcamentos.size());
        return ResponseEntity.ok(orcamentos);
    }

    @PutMapping("/{orcamentoId}")
    @Operation(
            summary = "Alterar orçamento",
            description = "Atualiza os dados de um orçamento existente a partir do número do orçamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orçamento alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração", content = @Content),
            @ApiResponse(responseCode = "404", description = "Orçamento ou cliente não encontrado", content = @Content)
    })
    public ResponseEntity<Void> alterar(
            @PathVariable String orcamentoId,
            @RequestBody AlterarOrcamentoRequest request) {
        log.info("Recebida requisicao de alteracao de orcamento. numeroOrcamento={}, clienteId={}, ordemDeServicoId={}",
                orcamentoId, request.clienteId(), request.ordemDeServicoId());
        alterarOrcamentoUseCase.alterarOrcamento(new AlterarOrcamentoCommand(
                orcamentoId,
                validarClienteId(request.clienteId()),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.placaVeiculo(),
                request.marcaVeiculo(),
                request.modeloVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                request.validade(),
                request.observacoes()));
        log.info("Requisicao de alteracao de orcamento concluida. numeroOrcamento={}", orcamentoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orcamentoId}")
    @Operation(summary = "Excluir orçamento", description = "Remove um orçamento pelo número do orçamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orçamento excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable String orcamentoId) {
        log.info("Recebida requisicao de exclusao de orcamento. numeroOrcamento={}", orcamentoId);
        excluirOrcamentoUseCase.excluirOrcamento(new ExcluirOrcamentoCommand(orcamentoId));
        log.info("Requisicao de exclusao de orcamento concluida. numeroOrcamento={}", orcamentoId);
        return ResponseEntity.noContent().build();
    }

    private String validarClienteId(String clienteId) {
        try {
            return UUID.fromString(clienteId).toString();
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException("Identificador do cliente invalido.");
        }
    }
}
