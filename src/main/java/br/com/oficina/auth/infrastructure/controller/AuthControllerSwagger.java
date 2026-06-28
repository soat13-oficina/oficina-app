package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.infrastructure.controller.dto.LoginRequest;
import br.com.oficina.auth.infrastructure.controller.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação", description = "Geração de token JWT para acesso administrativo")
public interface AuthControllerSwagger {

    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna um token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token JWT emitido"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (email ausente/inválido ou senha em branco)"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    ResponseEntity<LoginResponse> login(LoginRequest request);

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário criado e token JWT emitido"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (email ausente/inválido ou senha em branco)"),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado"),
            @ApiResponse(responseCode = "500", description = "Erro inesperado ao processar o registro")
    })
    ResponseEntity<LoginResponse> register(LoginRequest request);
}
