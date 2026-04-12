package br.com.oficina.infrastructure.rest;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.application.orcamento.AlterarOrcamentoRequest;
import br.com.oficina.application.orcamento.AlterarOrcamentoUseCase;
import br.com.oficina.application.orcamento.CadastrarOrcamentoRequest;
import br.com.oficina.application.orcamento.CadastrarOrcamentoUseCase;
import br.com.oficina.application.orcamento.ConsultarOrcamentoUseCase;
import br.com.oficina.application.orcamento.ExcluirOrcamentoUseCase;
import br.com.oficina.application.orcamento.ListarOrcamentosUseCase;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {
    private final CadastrarOrcamentoUseCase cadastrarOrcamentoUseCase;
    private final ConsultarOrcamentoUseCase consultarOrcamentoUseCase;
    private final AlterarOrcamentoUseCase alterarOrcamentoUseCase;
    private final ExcluirOrcamentoUseCase excluirOrcamentoUseCase;
    private final ListarOrcamentosUseCase listarOrcamentosUseCase;

    public OrcamentoController(
            CadastrarOrcamentoUseCase cadastrarOrcamentoUseCase,
            ConsultarOrcamentoUseCase consultarOrcamentoUseCase,
            AlterarOrcamentoUseCase alterarOrcamentoUseCase,
            ExcluirOrcamentoUseCase excluirOrcamentoUseCase,
            ListarOrcamentosUseCase listarOrcamentosUseCase) {
        this.cadastrarOrcamentoUseCase = cadastrarOrcamentoUseCase;
        this.consultarOrcamentoUseCase = consultarOrcamentoUseCase;
        this.alterarOrcamentoUseCase = alterarOrcamentoUseCase;
        this.excluirOrcamentoUseCase = excluirOrcamentoUseCase;
        this.listarOrcamentosUseCase = listarOrcamentosUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarOrcamentoRequest request) {
        cadastrarOrcamentoUseCase.cadastrarOrcamento(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{orcamentoId}")
    public ResponseEntity<OrcamentoResponse> consultar(@PathVariable String orcamentoId) {
        return consultarOrcamentoUseCase.consultarOrcamento(
                        new ConsultarOrcamentoUseCase.ConsultarOrcamentoRequest(orcamentoId))
                .map(OrcamentoResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoResponse>> listar() {
        List<OrcamentoResponse> orcamentos = listarOrcamentosUseCase.listarOrcamentos()
                .stream()
                .map(OrcamentoResponse::from)
                .toList();
        return ResponseEntity.ok(orcamentos);
    }

    @PutMapping("/{orcamentoId}")
    public ResponseEntity<Void> alterar(
            @PathVariable String orcamentoId,
            @RequestBody AlterarOrcamentoRequest request) {
        alterarOrcamentoUseCase.alterarOrcamento(new AlterarOrcamentoUseCase.AlterarOrcamentoRequest(
                orcamentoId,
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.clienteId(),
                request.placaVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                request.validade(),
                request.observacoes()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orcamentoId}")
    public ResponseEntity<Void> excluir(@PathVariable String orcamentoId) {
        excluirOrcamentoUseCase.excluirOrcamento(new ExcluirOrcamentoUseCase.ExcluirOrcamentoRequest(orcamentoId));
        return ResponseEntity.noContent().build();
    }
}
