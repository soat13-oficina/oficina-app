package br.com.oficina.veiculo.infrastructure.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.veiculo.infrastructure.web.request.CadastrarVeiculoRequest;

@RestController
@RequestMapping("/veiculos")
public class VeiculoController {
    private final CadastrarVeiculoUseCase cadastrarVeiculoUseCase;

    public VeiculoController(CadastrarVeiculoUseCase cadastrarVeiculoUseCase) {
        this.cadastrarVeiculoUseCase = cadastrarVeiculoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarVeiculoRequest request) {
        cadastrarVeiculoUseCase.cadastrarVeiculo(
                new CadastrarVeiculoCommand(request.placa(), request.marca(), request.modelo(), request.clienteId()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{placa}")
                .buildAndExpand(request.placa())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
