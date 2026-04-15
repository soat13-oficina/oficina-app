package br.com.oficina.pecainsumo.infrastructure.web;

import java.net.URI;
import java.util.List;

import br.com.oficina.pecainsumo.application.usecase.*;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
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

import br.com.oficina.pecainsumo.application.command.AdicionarEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.command.AlterarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.CadastrarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.ConsumirPecaCommand;
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.infrastructure.web.request.AdicionarEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.AlterarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.CadastrarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.ConsumirPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.LiberarReservaPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.RemoverEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.ReservarPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.response.PecaInsumoResponse;

@RestController
@RequestMapping("/pecas-insumos")
@Tag(name = "Peças e Insumos", description = "Operações de cadastro, consulta, alteração, exclusão e gestão de estoque de peças e insumos")
@SecurityRequirement(name = "bearerAuth")
public class PecaInsumoController {
    private final AlterarPecaInsumoUseCase alterarPecaInsumoUseCase;
    private final CadastrarPecaInsumoUseCase cadastrarPecaInsumoUseCase;
    private final ExcluirPecaInsumoUseCase excluirPecaInsumoUseCase;
    private final ListarPecasInsumosUseCase listarPecasInsumosUseCase;
    private final BuscarPecaInsumoPorIdUseCase buscarPecaInsumoPorIdUseCase;
    private final AdicionarEstoquePecaUseCase adicionarEstoquePecaUseCase;
    private final RemoverEstoquePecaUseCase removerEstoquePecaUseCase;
    private final ReservarPecaUseCase reservarPecaUseCase;
    private final LiberarReservaPecaUseCase liberarReservaPecaUseCase;
    private final ConsumirPecaUseCase consumirPecaUseCase;

    public PecaInsumoController(
            AlterarPecaInsumoUseCase alterarPecaInsumoUseCase,
            CadastrarPecaInsumoUseCase cadastrarPecaInsumoUseCase,
            ExcluirPecaInsumoUseCase excluirPecaInsumoUseCase,
            ListarPecasInsumosUseCase listarPecasInsumosUseCase,
            BuscarPecaInsumoPorIdUseCase buscarPecaInsumoPorIdUseCase,
            AdicionarEstoquePecaUseCase adicionarEstoquePecaUseCase,
            RemoverEstoquePecaUseCase removerEstoquePecaUseCase,
            ReservarPecaUseCase reservarPecaUseCase,
            LiberarReservaPecaUseCase liberarReservaPecaUseCase,
            ConsumirPecaUseCase consumirPecaUseCase) {
        this.alterarPecaInsumoUseCase = alterarPecaInsumoUseCase;
        this.cadastrarPecaInsumoUseCase = cadastrarPecaInsumoUseCase;
        this.excluirPecaInsumoUseCase = excluirPecaInsumoUseCase;
        this.listarPecasInsumosUseCase = listarPecasInsumosUseCase;
        this.buscarPecaInsumoPorIdUseCase = buscarPecaInsumoPorIdUseCase;
        this.adicionarEstoquePecaUseCase = adicionarEstoquePecaUseCase;
        this.removerEstoquePecaUseCase = removerEstoquePecaUseCase;
        this.reservarPecaUseCase = reservarPecaUseCase;
        this.liberarReservaPecaUseCase = liberarReservaPecaUseCase;
        this.consumirPecaUseCase = consumirPecaUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar peça/insumo", description = "Cria uma nova peça ou insumo no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Peça/insumo cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para cadastro", content = @Content)
    })
    public ResponseEntity<Void> cadastrar(@Valid @RequestBody CadastrarPecaInsumoRequest request) {
        CadastrarPecaInsumoCommand command = new CadastrarPecaInsumoCommand(
                request.descricao(),
                request.marca(),
                request.preco(),
                request.quantidadeEstoque(),
                request.codigoReferencia(),
                request.categoria());
        cadastrarPecaInsumoUseCase.cadastrarPecaInsumo(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("")
                .build()
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar peça/insumo", description = "Atualiza os dados de uma peça ou insumo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça/insumo alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para alteração", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> alterar(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody AlterarPecaInsumoRequest request) {
        alterarPecaInsumoUseCase.alterarPecaInsumo(new AlterarPecaInsumoCommand(
                id,
                request.descricao(),
                request.marca(),
                request.preco(),
                request.quantidadeEstoque(),
                request.quantidadeReservada(),
                request.codigoReferencia(),
                request.categoria()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça/insumo por ID", description = "Consulta uma peça ou insumo pelo identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça/insumo encontrada",
                    content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<PecaInsumoResponse> buscarPorId(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id) {
        PecaInsumoResponse response = buscarPecaInsumoPorIdUseCase
                .buscar(id)
                .map(PecaInsumoResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Peça/Insumo não encontrada com o ID: " + id));

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar peças/insumos", description = "Lista todas as peças e insumos, com filtros opcionais por marca, categoria e reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de peças/insumos retornada com sucesso")
    })
    public ResponseEntity<List<PecaInsumoResponse>> listar(
            @Parameter(description = "Filtrar por marca", example = "Bosch")
            @RequestParam(required = false) String marca,
            @Parameter(description = "Filtrar por categoria", example = "FILTROS")
            @RequestParam(required = false) CategoriaPeca categoria,
            @Parameter(description = "Filtrar por peças que possuem reserva (true/false)")
            @RequestParam(required = false) Boolean possuiReserva) {
        List<PecaInsumoResponse> pecasInsumos = listarPecasInsumosUseCase
                .listarPecasInsumos(new ListarPecasInsumosQuery(marca, categoria, possuiReserva))
                .stream()
                .map(PecaInsumoResponse::from)
                .toList();
        return ResponseEntity.ok(pecasInsumos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir peça/insumo", description = "Remove uma peça ou insumo pelo identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça/insumo excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id) {
        excluirPecaInsumoUseCase.excluirPecaInsumo(new ExcluirPecaInsumoCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/adicionar-estoque")
    @Operation(summary = "Adicionar estoque", description = "Adiciona uma quantidade ao estoque de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estoque adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> adicionarEstoque(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody AdicionarEstoquePecaRequest request) {
        adicionarEstoquePecaUseCase.adicionarEstoque(new AdicionarEstoquePecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/remover-estoque")
    @Operation(summary = "Remover estoque", description = "Remove uma quantidade do estoque de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estoque removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou estoque insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> removerEstoque(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody RemoverEstoquePecaRequest request) {
        removerEstoquePecaUseCase.removerEstoque(new RemoverEstoquePecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reservar")
    @Operation(summary = "Reservar peça", description = "Reserva uma quantidade de uma peça/insumo do estoque disponível.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou estoque disponível insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> reservar(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody ReservarPecaRequest request) {
        reservarPecaUseCase.reservarPeca(new ReservarPecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/liberar-reserva")
    @Operation(summary = "Liberar reserva", description = "Libera uma quantidade reservada de uma peça/insumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva liberada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou reserva insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> liberarReserva(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody LiberarReservaPecaRequest request) {
        liberarReservaPecaUseCase.liberarReserva(new LiberarReservaPecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/consumir")
    @Operation(summary = "Consumir peça reservada", description = "Consome uma quantidade de peças previamente reservadas, removendo-as definitivamente do estoque.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça consumida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou reserva insuficiente para consumo", content = @Content),
            @ApiResponse(responseCode = "404", description = "Peça/insumo não encontrada", content = @Content)
    })
    public ResponseEntity<Void> consumir(
            @Parameter(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
            @PathVariable String id,
            @Valid @RequestBody ConsumirPecaRequest request) {
        consumirPecaUseCase.consumirPeca(new ConsumirPecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }
}
