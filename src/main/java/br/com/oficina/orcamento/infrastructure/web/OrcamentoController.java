package br.com.oficina.orcamento.infrastructure.web;

import java.net.URI;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
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

import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.command.ExcluirOrcamentoCommand;
import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.application.usecase.AlterarOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.ConsultarOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.ExcluirOrcamentoUseCase;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.infrastructure.web.request.AlterarOrcamentoRequest;
import br.com.oficina.orcamento.infrastructure.web.request.CadastrarOrcamentoRequest;
import br.com.oficina.orcamento.infrastructure.web.response.OrcamentoResponse;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {
    private final CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase;
    private final ConsultarOrcamentoUseCase consultarOrcamentoUseCase;
    private final AlterarOrcamentoUseCase alterarOrcamentoUseCase;
    private final ExcluirOrcamentoUseCase excluirOrcamentoUseCase;

    public OrcamentoController(
            CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase,
            ConsultarOrcamentoUseCase consultarOrcamentoUseCase,
            AlterarOrcamentoUseCase alterarOrcamentoUseCase,
            ExcluirOrcamentoUseCase excluirOrcamentoUseCase) {
        this.cadastrarNovoOrcamentoUseCase = cadastrarNovoOrcamentoUseCase;
        this.consultarOrcamentoUseCase = consultarOrcamentoUseCase;
        this.alterarOrcamentoUseCase = alterarOrcamentoUseCase;
        this.excluirOrcamentoUseCase = excluirOrcamentoUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarOrcamentoRequest request) {
        cadastrarNovoOrcamentoUseCase.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                request.numeroOrcamento(),
                request.clienteId(),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.placaVeiculo(),
                request.marcaVeiculo(),
                request.modeloVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                request.validade(),
                request.observacoes()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.numeroOrcamento())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{orcamentoId}")
    public ResponseEntity<OrcamentoResponse> consultar(@PathVariable String orcamentoId) {
        OrcamentoResponse response = consultarOrcamentoUseCase
                .consultarOrcamento(new ConsultarOrcamentoQuery(orcamentoId, null, null))
                .stream()
                .findFirst()
                .map(OrcamentoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoResponse>> consultarPorFiltros(
            @RequestParam(required = false) String numeroOrcamento,
            @RequestParam(required = false) String cpfCliente,
            @RequestParam(required = false) String placaVeiculo) {
        List<OrcamentoResponse> orcamentos = consultarOrcamentoUseCase
                .consultarOrcamento(new ConsultarOrcamentoQuery(numeroOrcamento, cpfCliente, placaVeiculo))
                .stream()
                .map(OrcamentoResponse::from)
                .toList();
        return ResponseEntity.ok(orcamentos);
    }

    @PutMapping("/{orcamentoId}")
    public ResponseEntity<Void> alterar(
            @PathVariable String orcamentoId,
            @RequestBody AlterarOrcamentoRequest request) {
        alterarOrcamentoUseCase.alterarOrcamento(new AlterarOrcamentoCommand(
                orcamentoId,
                request.clienteId(),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.placaVeiculo(),
                request.marcaVeiculo(),
                request.modeloVeiculo(),
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
        excluirOrcamentoUseCase.excluirOrcamento(new ExcluirOrcamentoCommand(orcamentoId));
        return ResponseEntity.noContent().build();
    }
}
