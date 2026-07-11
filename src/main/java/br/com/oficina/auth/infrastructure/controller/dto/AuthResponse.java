package br.com.oficina.auth.infrastructure.controller.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * Corpo de resposta da autenticação JWT bem-sucedida (login e registro).
 *
 * <p>Contrato autodescritivo: além do {@code token}, expõe o esquema de autorização
 * ({@code tokenType}) e a validade ({@code expiresIn} em segundos e {@code expiresAt} como instante
 * absoluto). Não carrega dados sensíveis (senha, hash, segredo de assinatura ou id interno).
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String email) {

    private static final String TIPO_TOKEN_BEARER = "Bearer";

    /**
     * Constrói a resposta derivando os campos da fonte de verdade (token e sua expiração).
     *
     * @param token     token JWT assinado
     * @param email     identidade pública do usuário autenticado
     * @param expiresAt instante absoluto de expiração extraído do próprio token
     */
    public static AuthResponse from(String token, String email, Instant expiresAt) {
        long expiresIn = Duration.between(Instant.now(), expiresAt).getSeconds();
        return new AuthResponse(token, TIPO_TOKEN_BEARER, expiresIn, expiresAt, email);
    }
}
