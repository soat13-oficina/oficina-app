package br.com.oficina.ordemservico.infrastructure.web;

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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.application.query.ConsultarFuncionarioQuery;
import br.com.oficina.ordemservico.application.query.ListarFuncionariosQuery;
import br.com.oficina.ordemservico.application.usecase.AlterarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.CadastrarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ExcluirFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ListarFuncionariosUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.AlterarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CadastrarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.FuncionarioResponse;

@RestController
@RequestMapping("/funcionarios")
@Tag(name = "Funcionários", description = "Operações de cadastro, consulta, alteração e exclusão de funcionários")
@SecurityRequirement(name = "bearerAuth")
public class FuncionarioController {
    private static final Logger log = LoggerFactory.getLogger(FuncionarioController.class);

    private final CadastrarFuncionarioUseCase cadastrarFuncionarioUseCase;
    private final AlterarFuncionarioUseCase alterarFuncionarioUseCase;
    private final ExcluirFuncionarioUseCase excluirFuncionarioUseCase;
    private final ConsultarFuncionarioUseCase consultarFuncionarioUseCase;
    private final ListarFuncionariosUseCase listarFuncionariosUseCase;

    public FuncionarioController(
            CadastrarFuncionarioUseCase cadastrarFuncionarioUseCase,
            AlterarFuncionarioUseCase alterarFuncionarioUseCase,
            ExcluirFuncionarioUseCase excluirFuncionarioUseCase,
            ConsultarFuncionarioUseCase consultarFuncionarioUseCase,
            ListarFuncionariosUseCase listarFuncionariosUseCase) {
        this.cadastrarFuncionarioUseCase = cadastrarFuncionarioUseCase;
        this.alterarFuncionarioUseCase = alterarFuncionarioUseCase;
        this.excluirFuncionarioUseCase = excluirFuncionarioUseCase;
        this.consultarFuncionarioUseCase = consultarFuncionarioUseCase;
        this.listarFuncionariosUseCase = listarFuncionariosUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar funcionário",
            description = "Cria um novo funcionário. O nome é obrigatório. O CPF é opcional, mas quando informado deve conter 11 dígitos e não pode estar associado a outro funcionário já cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro ou CPF já cadastrado", content = @Content)
    })
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarFuncionarioRequest request) {
        log.info("Recebida requisicao de cadastro de funcionario. cpfInformado={}", request.cpf() != null && !request.cpf().isBlank());
        UUID funcionarioId = cadastrarFuncionarioUseCase.cadastrarFuncionario(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(funcionarioId)
                .toUri();
        log.info("Requisicao de cadastro de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    @Operation(
            summary = "Listar funcionários",
            description = "Lista funcionários ordenados por nome. Aceita filtro opcional pelo nome (busca parcial, sem distinção de maiúsculas ou acentos).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionários retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FuncionarioResponse.class))))
    })
    public ResponseEntity<List<FuncionarioResponse>> listar(
            @Parameter(description = "Filtro opcional pelo nome do funcionário. Ex.: `João`, `silva`.")
            @RequestParam(required = false) String nome) {
        log.info("Recebida requisicao de listagem de funcionarios. filtroNome={}", nome != null && !nome.isBlank());
        List<FuncionarioResponse> funcionarios = listarFuncionariosUseCase.listarFuncionarios(new ListarFuncionariosQuery(nome))
                .stream()
                .map(FuncionarioResponse::from)
                .toList();
        log.info("Requisicao de listagem de funcionarios concluida. quantidade={}", funcionarios.size());
        return ResponseEntity.ok(funcionarios);
    }

    @GetMapping("/{funcionarioId}")
    @Operation(summary = "Consultar funcionário", description = "Consulta um funcionário pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionário encontrado",
                    content = @Content(schema = @Schema(implementation = FuncionarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    public ResponseEntity<FuncionarioResponse> consultar(@PathVariable String funcionarioId) {
        log.info("Recebida requisicao de consulta de funcionario. funcionarioId={}", funcionarioId);
        FuncionarioResponse response = consultarFuncionarioUseCase
                .consultarFuncionario(new ConsultarFuncionarioQuery(paraUuid(funcionarioId)))
                .map(FuncionarioResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        log.info("Requisicao de consulta de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{funcionarioId}")
    @Operation(
            summary = "Alterar funcionário",
            description = "Atualiza os dados de um funcionário existente. O nome continua obrigatório. O CPF permanece opcional, mas quando informado deve conter 11 dígitos e não pode pertencer a outro funcionário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionário alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou CPF já cadastrado para outro funcionário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    public ResponseEntity<Void> alterar(@PathVariable String funcionarioId, @RequestBody AlterarFuncionarioRequest request) {
        log.info("Recebida requisicao de alteracao de funcionario. funcionarioId={}, cpfInformado={}",
                funcionarioId, request.cpf() != null && !request.cpf().isBlank());
        alterarFuncionarioUseCase.alterarFuncionario(request.toCommand(paraUuid(funcionarioId)));
        log.info("Requisicao de alteracao de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{funcionarioId}")
    @Operation(summary = "Excluir funcionário", description = "Remove um funcionário pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionário excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable String funcionarioId) {
        log.info("Recebida requisicao de exclusao de funcionario. funcionarioId={}", funcionarioId);
        excluirFuncionarioUseCase.excluirFuncionario(new ExcluirFuncionarioCommand(paraUuid(funcionarioId)));
        log.info("Requisicao de exclusao de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.noContent().build();
    }

    private UUID paraUuid(String funcionarioId) {
        try {
            return UUID.fromString(funcionarioId);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException("Identificador do funcionario invalido.");
        }
    }
}
