package br.com.oficina.ordemservico.infrastructure.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.application.query.ConsultarFuncionarioQuery;
import br.com.oficina.ordemservico.application.query.ListarFuncionariosQuery;
import br.com.oficina.ordemservico.application.usecase.AlterarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.CadastrarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ExcluirFuncionarioUseCase;
import br.com.oficina.ordemservico.application.usecase.ListarFuncionariosUseCase;
import br.com.oficina.ordemservico.infrastructure.web.request.AlterarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CadastrarFuncionarioRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.FuncionarioResponse;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController implements FuncionarioControllerSwagger {
    private static final Logger log = LoggerFactory.getLogger(FuncionarioController.class);

    private final CadastrarFuncionarioUseCase cadastrarFuncionarioUseCase;
    private final AlterarFuncionarioUseCase alterarFuncionarioUseCase;
    private final ExcluirFuncionarioUseCase excluirFuncionarioUseCase;
    private final ConsultarFuncionarioUseCase consultarFuncionarioUseCase;
    private final ListarFuncionariosUseCase listarFuncionariosUseCase;

    public FuncionarioController(
            CadastrarFuncionarioUseCase cadastrarFuncionarioUseCase,
            AlterarFuncionarioUseCase alterarFuncionarioUseCase,
            ExcluirFuncionarioUseCase excluirFuncionarioUseCase,
            ConsultarFuncionarioUseCase consultarFuncionarioUseCase,
            ListarFuncionariosUseCase listarFuncionariosUseCase) {
        this.cadastrarFuncionarioUseCase = cadastrarFuncionarioUseCase;
        this.alterarFuncionarioUseCase = alterarFuncionarioUseCase;
        this.excluirFuncionarioUseCase = excluirFuncionarioUseCase;
        this.consultarFuncionarioUseCase = consultarFuncionarioUseCase;
        this.listarFuncionariosUseCase = listarFuncionariosUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarFuncionarioRequest request) {
        log.info("Recebida requisicao de cadastro de funcionario. cpfInformado={}", request.cpf() != null && !request.cpf().isBlank());
        UUID funcionarioId = cadastrarFuncionarioUseCase.cadastrarFuncionario(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(funcionarioId)
                .toUri();
        log.info("Requisicao de cadastro de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<List<FuncionarioResponse>> listar(
            @RequestParam(required = false) String nome) {
        log.info("Recebida requisicao de listagem de funcionarios. filtroNome={}", nome != null && !nome.isBlank());
        List<FuncionarioResponse> funcionarios = listarFuncionariosUseCase.listarFuncionarios(new ListarFuncionariosQuery(nome))
                .stream()
                .map(FuncionarioResponse::from)
                .toList();
        log.info("Requisicao de listagem de funcionarios concluida. quantidade={}", funcionarios.size());
        return ResponseEntity.ok(funcionarios);
    }

    @GetMapping("/{funcionarioId}")
    public ResponseEntity<FuncionarioResponse> consultar(@PathVariable String funcionarioId) {
        log.info("Recebida requisicao de consulta de funcionario. funcionarioId={}", funcionarioId);
        FuncionarioResponse response = consultarFuncionarioUseCase
                .consultarFuncionario(new ConsultarFuncionarioQuery(paraUuid(funcionarioId)))
                .map(FuncionarioResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        log.info("Requisicao de consulta de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{funcionarioId}")
    public ResponseEntity<Void> alterar(@PathVariable String funcionarioId, @RequestBody AlterarFuncionarioRequest request) {
        log.info("Recebida requisicao de alteracao de funcionario. funcionarioId={}, cpfInformado={}",
                funcionarioId, request.cpf() != null && !request.cpf().isBlank());
        alterarFuncionarioUseCase.alterarFuncionario(request.toCommand(paraUuid(funcionarioId)));
        log.info("Requisicao de alteracao de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{funcionarioId}")
    public ResponseEntity<Void> excluir(@PathVariable String funcionarioId) {
        log.info("Recebida requisicao de exclusao de funcionario. funcionarioId={}", funcionarioId);
        excluirFuncionarioUseCase.excluirFuncionario(new ExcluirFuncionarioCommand(paraUuid(funcionarioId)));
        log.info("Requisicao de exclusao de funcionario concluida. funcionarioId={}", funcionarioId);
        return ResponseEntity.noContent().build();
    }

    private UUID paraUuid(String funcionarioId) {
        try {
            return UUID.fromString(funcionarioId);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException("Identificador do funcionario invalido.");
        }
    }
}
