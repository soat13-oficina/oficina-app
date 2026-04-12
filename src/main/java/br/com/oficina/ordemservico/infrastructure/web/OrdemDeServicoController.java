package br.com.oficina.ordemservico.infrastructure.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarOrdensDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.CriarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.FinalizarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.OrdemDeServicoResponse;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemDeServicoController {
    private final CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    private final FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase;
    private final ConsultarOrdensDeServicoUseCase consultarOrdensDeServicoUseCase;

    public OrdemDeServicoController(
            CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase,
            IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase,
            FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase,
            ConsultarOrdensDeServicoUseCase consultarOrdensDeServicoUseCase) {
        this.criarOrdemDeServicoUseCase = criarOrdemDeServicoUseCase;
        this.iniciarDiagnosticoUseCase = iniciarDiagnosticoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
        this.finalizarOrdemDeServicoUseCase = finalizarOrdemDeServicoUseCase;
        this.consultarOrdensDeServicoUseCase = consultarOrdensDeServicoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> criar(@RequestBody CriarOrdemDeServicoRequest request) {
        criarOrdemDeServicoUseCase.criarOrdemDeServico(
                new CriarOrdemDeServicoCommand(request.clienteId(), request.funcionarioId(), request.placaVeiculo()));
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<OrdemDeServicoResponse>> consultar(
            @RequestParam(required = false) String numeroOrdemServico,
            @RequestParam(required = false) String nomeCliente,
            @RequestParam(required = false) String placaVeiculo,
            @RequestParam(required = false) String cpfCliente) {
        List<OrdemDeServicoResponse> response = consultarOrdensDeServicoUseCase.consultarOrdensDeServico(
                        new ConsultarOrdensDeServicoQuery(
                                numeroOrdemServico,
                                nomeCliente,
                                placaVeiculo,
                                cpfCliente))
                .stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/iniciar")
    public ResponseEntity<Void> iniciarDiagnostico(@PathVariable String numeroOrdemServico) {
        iniciarDiagnosticoUseCase.iniciarDiagnostico(new IniciarDiagnosticoCommand(numeroOrdemServico));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/diagnostico/concluir")
    public ResponseEntity<Void> concluirDiagnostico(@PathVariable String numeroOrdemServico) {
        concluirDiagnosticoUseCase.concluirDiagnostico(new ConcluirDiagnosticoCommand(numeroOrdemServico));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{numeroOrdemServico}/finalizacao")
    public ResponseEntity<Void> finalizar(@PathVariable String numeroOrdemServico) {
        finalizarOrdemDeServicoUseCase.finalizarOrdemDeServico(new FinalizarOrdemDeServicoCommand(numeroOrdemServico));
        return ResponseEntity.noContent().build();
    }
}
