package br.com.oficina.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Subject do token, sem presumir o que ele representa.
     *
     * <p>Nos tokens emitidos por {@link #generateToken(String)} o subject é o e-mail
     * do usuário; nos tokens emitidos pela Function Serverless de autenticação
     * (repositório {@code oficina-lambda-auth}) é o CPF do cliente. {@code extractEmail}
     * continua existindo para o primeiro caso, onde o nome descreve o conteúdo.
     */
    public String extractSubject(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Claim {@code tipo}, que distingue a origem do token.
     *
     * <p>Vale {@code "CLIENTE"} nos tokens emitidos pela Lambda a partir do CPF, e é
     * ausente nos tokens de e-mail e senha desta aplicação. É por ela que o
     * {@link JwtAuthenticationFilter} decide se procura o usuário na base ou autentica
     * direto pelas claims.
     *
     * @return o tipo, ou {@code null} se o token não trouxer a claim.
     */
    public String extractTipo(String token) {
        return getClaims(token).get("tipo", String.class);
    }

    public String extractClienteId(String token) {
        return getClaims(token).get("clienteId", String.class);
    }

    public String extractNome(String token) {
        return getClaims(token).get("nome", String.class);
    }

    /**
     * Papéis declarados no token, sem o prefixo {@code ROLE_}.
     *
     * @return lista possivelmente vazia — nunca {@code null}.
     */
    public List<String> extractRoles(String token) {
        Object roles = getClaims(token).get("roles");

        if (roles instanceof Collection<?> colecao) {
            return colecao.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(papel -> !papel.isBlank())
                    .toList();
        }

        return List.of();
    }

    public Instant extractExpiration(String token) {
        return getClaims(token).getExpiration().toInstant();
    }

    public boolean validateToken(String token, String username) {
        return (username.equals(extractEmail(token)) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
