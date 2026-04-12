package br.com.oficina.pecainsumo.infrastructure.web;

import java.net.URI;
import java.util.List;

import br.com.oficina.pecainsumo.application.usecase.*;
import jakarta.persistence.EntityNotFoundException;
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
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.infrastructure.web.request.AdicionarEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.AlterarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.CadastrarPecaInsumoRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.LiberarReservaPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.RemoverEstoquePecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.request.ReservarPecaRequest;
import br.com.oficina.pecainsumo.infrastructure.web.response.PecaInsumoResponse;

@RestController
@RequestMapping("/pecas-insumos")
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

    public PecaInsumoController(
            AlterarPecaInsumoUseCase alterarPecaInsumoUseCase,
            CadastrarPecaInsumoUseCase cadastrarPecaInsumoUseCase,
            ExcluirPecaInsumoUseCase excluirPecaInsumoUseCase,
            ListarPecasInsumosUseCase listarPecasInsumosUseCase,
            BuscarPecaInsumoPorIdUseCase buscarPecaInsumoPorIdUseCase,
            AdicionarEstoquePecaUseCase adicionarEstoquePecaUseCase,
            RemoverEstoquePecaUseCase removerEstoquePecaUseCase,
            ReservarPecaUseCase reservarPecaUseCase,
            LiberarReservaPecaUseCase liberarReservaPecaUseCase) {
        this.alterarPecaInsumoUseCase = alterarPecaInsumoUseCase;
        this.cadastrarPecaInsumoUseCase = cadastrarPecaInsumoUseCase;
        this.excluirPecaInsumoUseCase = excluirPecaInsumoUseCase;
        this.listarPecasInsumosUseCase = listarPecasInsumosUseCase;
        this.buscarPecaInsumoPorIdUseCase = buscarPecaInsumoPorIdUseCase;
        this.adicionarEstoquePecaUseCase = adicionarEstoquePecaUseCase;
        this.removerEstoquePecaUseCase = removerEstoquePecaUseCase;
        this.reservarPecaUseCase = reservarPecaUseCase;
        this.liberarReservaPecaUseCase = liberarReservaPecaUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarPecaInsumoRequest request) {
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
    public ResponseEntity<Void> alterar(@PathVariable String id, @RequestBody AlterarPecaInsumoRequest request) {
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
    public ResponseEntity<PecaInsumoResponse> buscarPorId(@PathVariable String id) {
        PecaInsumoResponse response = buscarPecaInsumoPorIdUseCase
                .buscar(id)
                .map(PecaInsumoResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Peça/Insumo não encontrada com o ID: " + id));

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PecaInsumoResponse>> listar(
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean possuiReserva) {
        List<PecaInsumoResponse> pecasInsumos = listarPecasInsumosUseCase
                .listarPecasInsumos(new ListarPecasInsumosQuery(marca, categoria, possuiReserva))
                .stream()
                .map(PecaInsumoResponse::from)
                .toList();
        return ResponseEntity.ok(pecasInsumos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        excluirPecaInsumoUseCase.excluirPecaInsumo(new ExcluirPecaInsumoCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/adicionar-estoque")
    public ResponseEntity<Void> adicionarEstoque(@PathVariable String id, @RequestBody AdicionarEstoquePecaRequest request) {
        adicionarEstoquePecaUseCase.adicionarEstoque(new AdicionarEstoquePecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/remover-estoque")
    public ResponseEntity<Void> removerEstoque(@PathVariable String id, @RequestBody RemoverEstoquePecaRequest request) {
        removerEstoquePecaUseCase.removerEstoque(new RemoverEstoquePecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reservar")
    public ResponseEntity<Void> reservar(@PathVariable String id, @RequestBody ReservarPecaRequest request) {
        reservarPecaUseCase.reservarPeca(new ReservarPecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/liberar-reserva")
    public ResponseEntity<Void> liberarReserva(@PathVariable String id, @RequestBody LiberarReservaPecaRequest request) {
        liberarReservaPecaUseCase.liberarReserva(new LiberarReservaPecaCommand(id, request.quantidade()));
        return ResponseEntity.noContent().build();
    }
}
