package br.com.oficina.pecainsumo.infrastructure.web;

import java.util.List;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.infrastructure.web.request.AdicionarEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.AlterarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.CadastrarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.ConsumirPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.LiberarReservaPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.RemoverEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.ReservarPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.response.PecaInsumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Peças e Insumos", description = "Operações de cadastro, consulta, alteração, exclusão e gestão de estoque de peças e insumos")
@SecurityRequirement(name = "bearerAuth")
public interface PecaInsumoControllerSwagger {

    @Operation(summary = "Cadastrar peça/insumo", description = "Cria uma nova peça ou insumo no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Peça/insumo cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro", content = @Content)
    })
    ResponseEntity<Void> cadastrar(CadastrarPecaInsumoRequest request);

    @Operation(summary = "Alterar peça/insumo", description = "Atualiza os dados de uma peça ou insumo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça/insumo alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> alterar(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            AlterarPecaInsumoRequest request);

    @Operation(summary = "Buscar peça/insumo por ID", description = "Consulta uma peça ou insumo pelo identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça/insumo encontrada",
                    content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<PecaInsumoResponse> buscarPorId(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id);

    @Operation(summary = "Listar peças/insumos", description = "Lista todas as peças e insumos, com filtros opcionais por marca, categoria e reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de peças/insumos retornada com sucesso")
    })
    ResponseEntity<List<PecaInsumoResponse>> listar(
            @Parameter(description = "Filtrar por marca", example = "Bosch") String marca,
            @Parameter(description = "Filtrar por categoria", example = "FILTROS") CategoriaPeca categoria,
            @Parameter(description = "Filtrar por peças que possuem reserva (true/false)") Boolean possuiReserva);

    @Operation(summary = "Excluir peça/insumo", description = "Remove uma peça ou insumo pelo identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça/insumo excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Peça/insumo possui reserva ativa", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id);

    @Operation(summary = "Adicionar estoque", description = "Adiciona uma quantidade ao estoque de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estoque adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> adicionarEstoque(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            AdicionarEstoquePecaRequest request);

    @Operation(summary = "Remover estoque", description = "Remove uma quantidade do estoque de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estoque removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou estoque insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> removerEstoque(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            RemoverEstoquePecaRequest request);

    @Operation(summary = "Reservar peça", description = "Reserva uma quantidade de uma peça/insumo do estoque disponível.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou estoque disponível insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> reservar(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            ReservarPecaRequest request);

    @Operation(summary = "Liberar reserva", description = "Libera uma quantidade reservada de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva liberada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou reserva insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> liberarReserva(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            LiberarReservaPecaRequest request);

    @Operation(summary = "Consumir peça reservada", description = "Consome uma quantidade de peças previamente reservadas, removendo-as definitivamente do estoque.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça consumida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou reserva insuficiente para consumo", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    ResponseEntity<Void> consumir(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            String id,
            ConsumirPecaRequest request);
}
