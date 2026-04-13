package br.com.oficina.cliente.infrastructure.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.application.query.PesquisarClientesQuery;
import br.com.oficina.cliente.application.usecase.AlterarClienteUseCase;
import br.com.oficina.cliente.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.cliente.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.cliente.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.cliente.application.usecase.PesquisarClientesUseCase;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.cliente.infrastructure.web.request.AlterarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.request.CadastrarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.response.ClienteResponse;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Operações de cadastro, consulta, alteração e exclusão de clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {
    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);

    private final AlterarClienteUseCase alterarClienteUseCase;
    private final CadastrarClienteUseCase cadastrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;
    private final ExcluirClienteUseCase excluirClienteUseCase;
    private final PesquisarClientesUseCase pesquisarClientesUseCase;

    public ClienteController(
            AlterarClienteUseCase alterarClienteUseCase,
            CadastrarClienteUseCase cadastrarClienteUseCase,
            ConsultarClienteUseCase consultarClienteUseCase,
            ExcluirClienteUseCase excluirClienteUseCase,
            PesquisarClientesUseCase pesquisarClientesUseCase) {
        this.alterarClienteUseCase = alterarClienteUseCase;
        this.cadastrarClienteUseCase = cadastrarClienteUseCase;
        this.consultarClienteUseCase = consultarClienteUseCase;
        this.excluirClienteUseCase = excluirClienteUseCase;
        this.pesquisarClientesUseCase = pesquisarClientesUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar cliente",
            description = "Cria um novo cliente pessoa física ou pessoa jurídica. O CPF/CNPJ deve ser único e não pode estar associado a outro cliente já cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro ou CPF/CNPJ já cadastrado", content = @Content)
    })
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarClienteRequest request) {
        log.info("Recebida requisicao de cadastro de cliente. tipoCliente={}, documentoInformado={}",
                request.tipoCliente(),
                request.cpfOuCnpj() != null && !request.cpfOuCnpj().isBlank());
        UUID clienteId = cadastrarClienteUseCase.cadastrarCliente(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(clienteId)
                .toUri();
        log.info("Requisicao de cadastro concluida. clienteId={}", clienteId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    @Operation(
            summary = "Pesquisar clientes",
            description = "Pesquisa clientes por CPF/CNPJ, nome completo, primeiro nome ou sobrenome. Quando o termo não é informado, retorna todos os clientes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clientes retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class))))
    })
    public ResponseEntity<List<ClienteResponse>> pesquisar(
            @Parameter(
                    description = "Termo opcional para pesquisa por CPF/CNPJ, nome completo, primeiro nome ou sobrenome. Ex.: `12345678901`, `Maria Silva`, `Maria`, `Silva`.")
            @RequestParam(required = false) String termo) {
        log.info("Recebida requisicao de pesquisa de clientes. termoInformado={}", termo != null && !termo.isBlank());
        List<ClienteResponse> clientes = pesquisarClientesUseCase.pesquisarClientes(new PesquisarClientesQuery(termo))
                .stream()
                .map(ClienteResponse::from)
                .toList();
        log.info("Requisicao de pesquisa concluida. quantidadeClientes={}", clientes.size());
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{clienteId}")
    @Operation(summary = "Consultar cliente", description = "Consulta um cliente pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    public ResponseEntity<ClienteResponse> consultar(@PathVariable String clienteId) {
        log.info("Recebida requisicao de consulta de cliente. clienteId={}", clienteId);
        return ResponseEntity.ok(consultarClienteUseCase.consultarCliente(new ConsultarClienteQuery(paraUuid(clienteId)))
                .map(ClienteResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado.")));
    }

    @PutMapping("/{clienteId}")
    @Operation(
            summary = "Alterar cliente",
            description = "Atualiza os dados de um cliente existente. O CPF/CNPJ informado deve continuar único e não pode pertencer a outro cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou CPF/CNPJ já cadastrado para outro cliente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    public ResponseEntity<Void> alterar(@PathVariable String clienteId, @RequestBody AlterarClienteRequest request) {
        log.info("Recebida requisicao de alteracao de cliente. clienteId={}, tipoCliente={}, documentoInformado={}",
                clienteId,
                request.tipoCliente(),
                request.cpfOuCnpj() != null && !request.cpfOuCnpj().isBlank());
        alterarClienteUseCase.alterarCliente(request.toCommand(paraUuid(clienteId)));
        log.info("Requisicao de alteracao concluida. clienteId={}", clienteId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clienteId}")
    @Operation(summary = "Excluir cliente", description = "Remove um cliente pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable String clienteId) {
        log.info("Recebida requisicao de exclusao de cliente. clienteId={}", clienteId);
        excluirClienteUseCase.excluirCliente(new ExcluirClienteCommand(paraUuid(clienteId)));
        log.info("Requisicao de exclusao concluida. clienteId={}", clienteId);
        return ResponseEntity.noContent().build();
    }

    private UUID paraUuid(String clienteId) {
        try {
            return UUID.fromString(clienteId);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException("Identificador do cliente invalido.");
        }
    }
}
