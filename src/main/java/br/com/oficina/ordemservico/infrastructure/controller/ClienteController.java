package br.com.oficina.ordemservico.infrastructure.controller;

import java.net.URI;

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

import br.com.oficina.ordemservico.application.usecase.AlterarClienteUseCase;
import br.com.oficina.ordemservico.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.ordemservico.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.ordemservico.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.ordemservico.infrastructure.controller.dto.AlterarClienteRequest;
import br.com.oficina.ordemservico.infrastructure.controller.dto.CadastrarClienteRequest;
import br.com.oficina.ordemservico.infrastructure.controller.dto.ClienteResponse;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    private final AlterarClienteUseCase alterarClienteUseCase;
    private final CadastrarClienteUseCase cadastrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;
    private final ExcluirClienteUseCase excluirClienteUseCase;

    public ClienteController(
            AlterarClienteUseCase alterarClienteUseCase,
            CadastrarClienteUseCase cadastrarClienteUseCase,
            ConsultarClienteUseCase consultarClienteUseCase,
            ExcluirClienteUseCase excluirClienteUseCase) {
        this.alterarClienteUseCase = alterarClienteUseCase;
        this.cadastrarClienteUseCase = cadastrarClienteUseCase;
        this.consultarClienteUseCase = consultarClienteUseCase;
        this.excluirClienteUseCase = excluirClienteUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarClienteRequest request) {
        cadastrarClienteUseCase
                .cadastrarCliente(new CadastrarClienteUseCase.CadastrarClienteRequest(request.id(), request.nome()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> consultar(@PathVariable String clienteId) {
        return consultarClienteUseCase.consultarCliente(new ConsultarClienteUseCase.ConsultarClienteRequest(clienteId))
                .map(ClienteResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{clienteId}")
    public ResponseEntity<Void> alterar(@PathVariable String clienteId, @RequestBody AlterarClienteRequest request) {
        alterarClienteUseCase.alterarCliente(new AlterarClienteUseCase.AlterarClienteRequest(clienteId, request.nome()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> excluir(@PathVariable String clienteId) {
        excluirClienteUseCase.excluirCliente(new ExcluirClienteUseCase.ExcluirClienteRequest(clienteId));
        return ResponseEntity.noContent().build();
    }
}
