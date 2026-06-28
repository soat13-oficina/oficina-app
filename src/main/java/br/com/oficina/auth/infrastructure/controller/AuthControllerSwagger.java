package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.infrastructure.controller.dto.AuthResponse;
import br.com.oficina.auth.infrastructure.controller.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação", description = "Geração de token JWT para acesso administrativo")
public interface AuthControllerSwagger {

    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna o token JWT com tipo (Bearer), validade e identidade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado: corpo com token, tokenType (Bearer), expiresIn, expiresAt e email"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (email ausente/inválido ou senha em branco)"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    ResponseEntity<AuthResponse> login(LoginRequest request);

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário e retorna o token JWT com tipo (Bearer), validade e identidade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário criado: corpo com token, tokenType (Bearer), expiresIn, expiresAt e email"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (email ausente/inválido ou senha em branco)"),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado"),
            @ApiResponse(responseCode = "500", description = "Erro inesperado ao processar o registro")
    })
    ResponseEntity<AuthResponse> register(LoginRequest request);
}
