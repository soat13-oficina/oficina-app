package br.com.oficina.cliente.infrastructure.web;

import java.util.List;

import br.com.oficina.cliente.infrastructure.web.request.AlterarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.request.CadastrarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.response.ClienteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Clientes", description = "Operações de cadastro, consulta, alteração e exclusão de clientes")
@SecurityRequirement(name = "bearerAuth")
public interface ClienteControllerSwagger {

    @Operation(
            summary = "Cadastrar cliente",
            description = "Cria um novo cliente. O nome é obrigatório. CPF/CNPJ e tipo do cliente são opcionais, mas quando um for informado o outro também deve ser informado. Se houver CPF/CNPJ, ele deve ser único e não pode estar associado a outro cliente já cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro ou CPF/CNPJ já cadastrado", content = @Content)
    })
    ResponseEntity<Void> cadastrar(CadastrarClienteRequest request);

    @Operation(
            summary = "Pesquisar clientes",
            description = "Pesquisa clientes por CPF/CNPJ, nome completo, primeiro nome ou sobrenome. Quando o termo não é informado, retorna todos os clientes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clientes retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class))))
    })
    ResponseEntity<List<ClienteResponse>> pesquisar(
            @Parameter(
                    description = "Termo opcional para pesquisa por CPF/CNPJ, nome completo, primeiro nome ou sobrenome. Ex.: `12345678901`, `Maria Silva`, `Maria`, `Silva`.")
            String termo);

    @Operation(summary = "Consultar cliente", description = "Consulta um cliente pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    ResponseEntity<ClienteResponse> consultar(String clienteId);

    @Operation(
            summary = "Alterar cliente",
            description = "Atualiza os dados de um cliente existente. O nome continua obrigatório. CPF/CNPJ e tipo do cliente permanecem opcionais, mas quando um for informado o outro também deve ser informado. Se houver CPF/CNPJ, ele deve continuar único e não pode pertencer a outro cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou CPF/CNPJ já cadastrado para outro cliente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    ResponseEntity<Void> alterar(String clienteId, AlterarClienteRequest request);

    @Operation(summary = "Excluir cliente", description = "Remove um cliente pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    ResponseEntity<Void> excluir(String clienteId);
}
