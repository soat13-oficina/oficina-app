package br.com.oficina.cliente.infrastructure.web;

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

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.application.usecase.AlterarClienteUseCase;
import br.com.oficina.cliente.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.cliente.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.cliente.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.cliente.infrastructure.web.request.AlterarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.request.CadastrarClienteRequest;
import br.com.oficina.cliente.infrastructure.web.response.ClienteResponse;

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
        String clienteId = cadastrarClienteUseCase.cadastrarCliente(new CadastrarClienteCommand(
                request.nome(),
                request.cpfOuCnpj(),
                request.tipoCliente()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(clienteId)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> consultar(@PathVariable String clienteId) {
        return consultarClienteUseCase.consultarCliente(new ConsultarClienteQuery(clienteId))
                .map(ClienteResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{clienteId}")
    public ResponseEntity<Void> alterar(@PathVariable String clienteId, @RequestBody AlterarClienteRequest request) {
        alterarClienteUseCase.alterarCliente(new AlterarClienteCommand(
                clienteId,
                request.nome(),
                request.cpfOuCnpj(),
                request.tipoCliente()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> excluir(@PathVariable String clienteId) {
        excluirClienteUseCase.excluirCliente(new ExcluirClienteCommand(clienteId));
        return ResponseEntity.noContent().build();
    }
}
