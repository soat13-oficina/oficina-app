package br.com.oficina.ordemservico.infrastructure.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.ordemservico.application.usecase.CadastrarVeiculoUseCase;

@RestController
@RequestMapping("/veiculos")
public class VeiculoController {
    private final CadastrarVeiculoUseCase cadastrarVeiculoUseCase;

    public VeiculoController(CadastrarVeiculoUseCase cadastrarVeiculoUseCase) {
        this.cadastrarVeiculoUseCase = cadastrarVeiculoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarVeiculoUseCase.CadastrarVeiculoRequest request) {
        cadastrarVeiculoUseCase.cadastrarVeiculo(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{placa}")
                .buildAndExpand(request.placa())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
