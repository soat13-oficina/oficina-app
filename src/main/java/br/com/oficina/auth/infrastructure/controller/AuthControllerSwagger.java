package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.infrastructure.controller.dto.LoginRequest;
import br.com.oficina.auth.infrastructure.controller.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação", description = "Geração de token JWT para acesso administrativo")
public interface AuthControllerSwagger {

    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna um token JWT")
    ResponseEntity<LoginResponse> login(LoginRequest request);

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário")
    ResponseEntity<LoginResponse> register(LoginRequest request);
}
