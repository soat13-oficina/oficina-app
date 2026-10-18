package br.com.oficina.cliente.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.cliente.domain.repository.ClienteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta interna de cliente por CPF, feita pela Lambda de autenticacao (oficina-lambda-auth) para
 * emitir o token do cliente: o OS Service e o dono do cadastro (ADR 0003) e a Lambda deixa de ler o
 * banco dele. Exige {@code X-Service-Token} (ver {@code ServiceTokenFilter}); contrato em
 * {@code docs/contratos/rest-interno.md}.
 */
@RestController
@Tag(name = "Interno", description = "Chamadas entre servicos, fora do API Gateway (header X-Service-Token)")
public class ConsultaInternaClientesController {

    private final ClienteRepository clientes;

    public ConsultaInternaClientesController(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Schema(name = "ClienteInternoResponse")
    public record ClienteInternoResponse(
            @Schema(description = "Id do cliente", example = "3f1c9a2e-0000-4000-8000-000000000001") String id,
            @Schema(description = "Nome do cliente", example = "Maria da Silva") String nome,
            @Schema(description = "Cliente ativo (so ele recebe token)", example = "true") boolean ativo) {
    }

    @Operation(summary = "Consulta o cliente pelo CPF",
            description = "Aceita o CPF com ou sem mascara; compara so os digitos, como o cadastro guarda o documento "
                    + "como foi digitado. Inativo tambem e devolvido (ativo=false): quem decide recusar o token e a Lambda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "400", description = "CPF nao tem 11 digitos", content = @Content),
            @ApiResponse(responseCode = "401", description = "X-Service-Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Nenhum cliente com esse CPF", content = @Content)
    })
    @GetMapping("/internal/clientes/por-cpf/{cpf}")
    public ResponseEntity<ClienteInternoResponse> porCpf(@PathVariable String cpf) {
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return clientes.buscarPorDocumento(digitos)
                .map(c -> ResponseEntity.ok(new ClienteInternoResponse(c.getId().toString(), c.getNome(), c.isAtivo())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
