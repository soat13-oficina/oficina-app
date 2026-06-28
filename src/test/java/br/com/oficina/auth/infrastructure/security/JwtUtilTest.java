package br.com.oficina.auth.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "CHAVE_SECRETA_DE_TESTE_LONGA_O_SUFICIENTE_PARA_HMAC_SHA256_2026";
    private static final long EXPIRACAO_MS = Duration.ofHours(24).toMillis();

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRACAO_MS);

    @Test
    void deveExtrairExpiracaoCoerenteComAJanelaConfigurada() {
        Instant antes = Instant.now();
        String token = jwtUtil.generateToken("admin@oficina.com");

        Instant expiracao = jwtUtil.extractExpiration(token);

        // A expiração deve cair próxima de antes + janela (tolerância para o relógio entre as chamadas).
        Instant esperadoMin = antes.plusMillis(EXPIRACAO_MS).minusSeconds(5);
        Instant esperadoMax = Instant.now().plusMillis(EXPIRACAO_MS).plusSeconds(5);

        assertTrue(!expiracao.isBefore(esperadoMin), "expiração não pode ser anterior à janela esperada");
        assertTrue(!expiracao.isAfter(esperadoMax), "expiração não pode exceder a janela esperada");
    }
}
