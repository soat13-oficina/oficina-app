package br.com.oficina.ordemservico.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.application.usecase.CriarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemDeServicoController {
    private final CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    private final FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase;

    public OrdemDeServicoController(
            CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase,
            IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase,
            FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase) {
        this.criarOrdemDeServicoUseCase = criarOrdemDeServicoUseCase;
        this.iniciarDiagnosticoUseCase = iniciarDiagnosticoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
        this.finalizarOrdemDeServicoUseCase = finalizarOrdemDeServicoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> criar(@RequestBody CriarOrdemDeServicoRequest request) {
        criarOrdemDeServicoUseCase.criarOrdemDeServico(
                new CriarOrdemDeServicoCommand(request.clienteId(), request.funcionarioId(), request.placaVeiculo()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{ordemDeServicoId}/diagnostico/iniciar")
    public ResponseEntity<Void> iniciarDiagnostico(@PathVariable String ordemDeServicoId) {
        iniciarDiagnosticoUseCase.iniciarDiagnostico(new IniciarDiagnosticoCommand(ordemDeServicoId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ordemDeServicoId}/diagnostico/concluir")
    public ResponseEntity<Void> concluirDiagnostico(@PathVariable String ordemDeServicoId) {
        concluirDiagnosticoUseCase.concluirDiagnostico(new ConcluirDiagnosticoCommand(ordemDeServicoId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ordemDeServicoId}/finalizacao")
    public ResponseEntity<Void> finalizar(@PathVariable String ordemDeServicoId) {
        finalizarOrdemDeServicoUseCase.finalizarOrdemDeServico(new FinalizarOrdemDeServicoCommand(ordemDeServicoId));
        return ResponseEntity.noContent().build();
    }
}
