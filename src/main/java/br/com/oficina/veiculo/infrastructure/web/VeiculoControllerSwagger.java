package br.com.oficina.veiculo.infrastructure.web;

import java.util.List;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.infrastructure.web.request.AlterarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.request.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.response.VeiculoResponse;
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

@Tag(name = "Veiculos", description = "Operações de cadastro, consulta, alteração e exclusão de veículos")
@SecurityRequirement(name = "bearerAuth")
public interface VeiculoControllerSwagger {

    @Operation(
            summary = "Cadastrar veiculo",
            description = "Cria um novo veículo vinculado a um cliente proprietário existente. A placa aceita formato Mercosul e formato antigo, e sempre é normalizada sem espaços, sem hífen e em caixa alta. Não são permitidos veículos com a mesma placa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro, placa inválida ou placa já cadastrada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente proprietário não encontrado", content = @Content)
    })
    ResponseEntity<Void> cadastrar(CadastrarVeiculoRequest request);

    @Operation(
            summary = "Alterar veiculo",
            description = "Atualiza os dados de um veículo existente identificado pela placa. A placa informada na URL pode ser enviada com espaços ou hífen, pois será normalizada antes da consulta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Veículo alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou placa inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content)
    })
    ResponseEntity<Void> alterar(String placa, AlterarVeiculoRequest request);

    @Operation(
            summary = "Consultar veiculos",
            description = "Consulta veículos por placa, marca, fabricante e demais filtros opcionais. A placa aceita formato Mercosul e formato antigo e é normalizada antes da pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículos retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VeiculoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro de placa inválido", content = @Content)
    })
    ResponseEntity<List<VeiculoResponse>> consultar(
            @Parameter(description = "Placa opcional para consulta. Aceita formatos como `ABC1D23`, `ABC-1234` ou `abc-1d23`.")
            String placa,
            @Parameter(description = "Ano do veículo.") Integer ano,
            @Parameter(description = "Marca do veículo.") String marca,
            @Parameter(description = "Fabricante do veículo.") String fabricante,
            @Parameter(description = "Potência do veículo.") Integer potencia,
            @Parameter(description = "Tipo de câmbio do veículo.") String cambio,
            @Parameter(description = "Tipo de combustível do veículo.") TipoCombustivel tipo);

    @Operation(
            summary = "Excluir veiculo",
            description = "Remove um veículo pela placa. A placa informada pode ser enviada com espaços ou hífen, pois será normalizada antes da exclusão.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Veículo excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Placa inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content)
    })
    ResponseEntity<Void> excluir(String placa);
}
