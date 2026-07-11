package br.com.oficina.auth.infrastructure.controller.dto;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthResponseTest {

    @Test
    void deveFixarTipoDeTokenComoBearer() {
        Instant expiraEm = Instant.now().plus(Duration.ofHours(24));

        AuthResponse resposta = AuthResponse.from("token-jwt", "admin@oficina.com", expiraEm);

        assertEquals("Bearer", resposta.tokenType());
    }

    @Test
    void devePropagarTokenEmailEExpiracao() {
        Instant expiraEm = Instant.now().plus(Duration.ofHours(24));

        AuthResponse resposta = AuthResponse.from("token-jwt", "admin@oficina.com", expiraEm);

        assertEquals("token-jwt", resposta.token());
        assertEquals("admin@oficina.com", resposta.email());
        assertEquals(expiraEm, resposta.expiresAt());
    }

    @Test
    void deveCalcularExpiresInDentroDaFaixaCoerenteComExpiresAt() {
        long janelaSegundos = Duration.ofHours(24).getSeconds();
        Instant expiraEm = Instant.now().plusSeconds(janelaSegundos);

        AuthResponse resposta = AuthResponse.from("token-jwt", "admin@oficina.com", expiraEm);

        // Faixa: janela-5s a janela (calculado com o relógio do sistema no from(...)), nunca valor exato.
        assertTrue(resposta.expiresIn() > 0, "expiresIn deve ser positivo");
        assertTrue(resposta.expiresIn() <= janelaSegundos
                        && resposta.expiresIn() >= janelaSegundos - 5,
                "expiresIn deve estar na faixa janela-5s a janela");
    }
}
