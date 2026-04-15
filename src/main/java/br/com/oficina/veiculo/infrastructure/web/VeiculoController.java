package br.com.oficina.veiculo.infrastructure.web;

import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.AlterarVeiculoUseCase;
import br.com.oficina.veiculo.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.application.usecase.ConsultarVeiculosUseCase;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.web.request.AlterarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.request.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.response.VeiculoResponse;

@RestController
@RequestMapping("/veiculos")
@Tag(name = "Veiculos", description = "Operações de cadastro, consulta, alteração e exclusão de veículos")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {
    private static final Logger log = LoggerFactory.getLogger(VeiculoController.class);

    private final AlterarVeiculoUseCase alterarVeiculoUseCase;
    private final CadastrarVeiculoUseCase cadastrarVeiculoUseCase;
    private final ConsultarVeiculosUseCase consultarVeiculosUseCase;
    private final ExcluirVeiculoUseCase excluirVeiculoUseCase;

    public VeiculoController(
            AlterarVeiculoUseCase alterarVeiculoUseCase,
            CadastrarVeiculoUseCase cadastrarVeiculoUseCase,
            ExcluirVeiculoUseCase excluirVeiculoUseCase,
            ConsultarVeiculosUseCase consultarVeiculosUseCase) {
        this.alterarVeiculoUseCase = alterarVeiculoUseCase;
        this.cadastrarVeiculoUseCase = cadastrarVeiculoUseCase;
        this.excluirVeiculoUseCase = excluirVeiculoUseCase;
        this.consultarVeiculosUseCase = consultarVeiculosUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar veiculo",
            description = "Cria um novo veículo vinculado a um cliente proprietário existente. A placa aceita formato Mercosul e formato antigo, e sempre é normalizada sem espaços, sem hífen e em caixa alta. Não são permitidos veículos com a mesma placa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro, placa inválida ou placa já cadastrada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente proprietário não encontrado", content = @Content)
    })
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarVeiculoRequest request) {
        log.info("Recebida requisicao de cadastro de veiculo. placaInformada={}, marca={}, fabricante={}",
                request.placa(),
                request.marca(),
                request.fabricante());
        cadastrarVeiculoUseCase.cadastrarVeiculo(new CadastrarVeiculoCommand(
                request.placa(),
                request.marca(),
                request.modelo(),
                request.fabricante(),
                request.ano(),
                request.potencia(),
                request.cambio(),
                request.tipo(),
                request.clienteId()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{placa}")
                .buildAndExpand(Veiculo.normalizarPlaca(request.placa()))
                .toUri();
        log.info("Requisicao de cadastro de veiculo concluida. location={}", location);
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{placa}")
    @Operation(
            summary = "Alterar veiculo",
            description = "Atualiza os dados de um veículo existente identificado pela placa. A placa informada na URL pode ser enviada com espaços ou hífen, pois será normalizada antes da consulta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Veículo alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração ou placa inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content)
    })
    public ResponseEntity<Void> alterar(@PathVariable String placa, @RequestBody AlterarVeiculoRequest request) {
        log.info("Recebida requisicao de alteracao de veiculo. placaInformada={}, marca={}, fabricante={}",
                placa,
                request.marca(),
                request.fabricante());
        alterarVeiculoUseCase.alterarVeiculo(new AlterarVeiculoCommand(
                placa,
                request.marca(),
                request.modelo(),
                request.fabricante(),
                request.ano(),
                request.potencia(),
                request.cambio(),
                request.tipo()));
        log.info("Requisicao de alteracao de veiculo concluida. placaInformada={}", placa);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Consultar veiculos",
            description = "Consulta veículos por placa, marca, fabricante e demais filtros opcionais. A placa aceita formato Mercosul e formato antigo e é normalizada antes da pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículos retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VeiculoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro de placa inválido", content = @Content)
    })
    public ResponseEntity<List<VeiculoResponse>> consultar(
            @Parameter(description = "Placa opcional para consulta. Aceita formatos como `ABC1D23`, `ABC-1234` ou `abc-1d23`.")
            @RequestParam(required = false) String placa,
            @Parameter(description = "Ano do veículo.")
            @RequestParam(required = false) Integer ano,
            @Parameter(description = "Marca do veículo.")
            @RequestParam(required = false) String marca,
            @Parameter(description = "Fabricante do veículo.")
            @RequestParam(required = false) String fabricante,
            @Parameter(description = "Potência do veículo.")
            @RequestParam(required = false) Integer potencia,
            @Parameter(description = "Tipo de câmbio do veículo.")
            @RequestParam(required = false) String cambio,
            @Parameter(description = "Tipo de combustível do veículo.")
            @RequestParam(required = false) TipoCombustivel tipo) {
        log.info("Recebida requisicao de consulta de veiculos. placaInformada={}, marca={}, fabricante={}, ano={}, tipo={}",
                placa,
                marca,
                fabricante,
                ano,
                tipo);
        List<VeiculoResponse> veiculos = consultarVeiculosUseCase
                .consultarVeiculos(new ConsultarVeiculosQuery(placa, ano, marca, fabricante, potencia, cambio, tipo))
                .stream()
                .map(VeiculoResponse::from)
                .toList();
        log.info("Requisicao de consulta de veiculos concluida. quantidadeVeiculos={}", veiculos.size());
        return ResponseEntity.ok(veiculos);
    }

    @DeleteMapping("/{placa}")
    @Operation(
            summary = "Excluir veiculo",
            description = "Remove um veículo pela placa. A placa informada pode ser enviada com espaços ou hífen, pois será normalizada antes da exclusão.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Veículo excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Placa inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable String placa) {
        log.info("Recebida requisicao de exclusao de veiculo. placaInformada={}", placa);
        excluirVeiculoUseCase.excluirVeiculo(new ExcluirVeiculoCommand(placa));
        log.info("Requisicao de exclusao de veiculo concluida. placaInformada={}", placa);
        return ResponseEntity.noContent().build();
    }
}
