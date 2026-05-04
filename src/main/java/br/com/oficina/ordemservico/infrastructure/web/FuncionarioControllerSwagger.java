package br.com.oficina.ordemservico.infrastructure.web;

import java.util.List;

import br.com.oficina.ordemservico.infrastructure.web.request.AlterarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CadastrarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.FuncionarioResponse;
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

@Tag(name = "Funcionários", description = "Operações de cadastro, consulta, alteração e exclusão de funcionários")
@SecurityRequirement(name = "bearerAuth")
public interface FuncionarioControllerSwagger {

    @Operation(
            summary = "Cadastrar funcionário",
            description = "Cria um novo funcionário. O nome é obrigatório. O CPF é opcional, mas quando informado deve conter 11 dígitos e não pode estar associado a outro funcionário já cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro ou CPF já cadastrado", content = @Content)
    })
    ResponseEntity<Void> cadastrar(CadastrarFuncionarioRequest request);

    @Operation(
            summary = "Listar funcionários",
            description = "Lista funcionários ordenados por nome. Aceita filtro opcional pelo nome (busca parcial, sem distinção de maiúsculas ou acentos).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionários retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FuncionarioResponse.class))))
    })
    ResponseEntity<List<FuncionarioResponse>> listar(
            @Parameter(description = "Filtro opcional pelo nome do funcionário. Ex.: `João`, `silva`.")
            String nome);

    @Operation(summary = "Consultar funcionário", description = "Consulta um funcionário pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionário encontrado",
                    content = @Content(schema = @Schema(implementation = FuncionarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    ResponseEntity<FuncionarioResponse> consultar(String funcionarioId);

    @Operation(
            summary = "Alterar funcionário",
            description = "Atualiza os dados de um funcionário existente. O nome continua obrigatório. O CPF permanece opcional, mas quando informado deve conter 11 dígitos e não pode pertencer a outro funcionário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionário alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou CPF já cadastrado para outro funcionário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    ResponseEntity<Void> alterar(String funcionarioId, AlterarFuncionarioRequest request);

    @Operation(summary = "Excluir funcionário", description = "Remove um funcionário pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionário excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado", content = @Content)
    })
    ResponseEntity<Void> excluir(String funcionarioId);
}
