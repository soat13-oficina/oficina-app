package br.com.oficina.infrastructure.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.application.diagnostico.ConcluirDiagnosticoUseCase;
import br.com.oficina.application.ordemservico.CriarOrdemDeServicoUseCase;
import br.com.oficina.application.diagnostico.IniciarDiagnosticoUseCase;
import br.com.oficina.application.ordemservico.CriarOrdemDeServicoRequest;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemDeServicoController {
    private final CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;

    public OrdemDeServicoController(
            CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase,
            IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase) {
        this.criarOrdemDeServicoUseCase = criarOrdemDeServicoUseCase;
        this.iniciarDiagnosticoUseCase = iniciarDiagnosticoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> criar(@RequestBody CriarOrdemDeServicoRequest request) {
        criarOrdemDeServicoUseCase.criarOrdemDeServico(
                new CriarOrdemDeServicoUseCase.CriarOrdemDeServicoRequest(
                        request.clienteId(),
                        request.funcionarioId(),
                        request.placaVeiculo()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{ordemDeServicoId}/diagnostico/iniciar")
    public ResponseEntity<Void> iniciarDiagnostico(@PathVariable String ordemDeServicoId) {
        iniciarDiagnosticoUseCase
                .iniciarDiagnostico(new IniciarDiagnosticoUseCase.IniciarDiagnosticoRequest(ordemDeServicoId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ordemDeServicoId}/diagnostico/concluir")
    public ResponseEntity<Void> concluirDiagnostico(@PathVariable String ordemDeServicoId) {
        concluirDiagnosticoUseCase
                .concluirDiagnostico(new ConcluirDiagnosticoUseCase.ConcluirDiagnosticoRequest(ordemDeServicoId));
        return ResponseEntity.noContent().build();
    }
}
