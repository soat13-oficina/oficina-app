package br.com.oficina.auth.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o ponto de integração entre esta aplicação e a Function Serverless de
 * autenticação: um token emitido pela Lambda a partir do CPF precisa autenticar aqui
 * sem que o cliente exista na tabela {@code usuarios}.
 */
class JwtAuthenticationFilterTest {

    // 64 caracteres, como o segredo que o Terraform gera em produção.
    private static final String SECRET = "CHAVE_SECRETA_DE_TESTE_LONGA_O_SUFICIENTE_PARA_HMAC_SHA512_2026x";
    private static final long EXPIRACAO_MS = Duration.ofHours(24).toMillis();

    private static final String CPF = "52998224725";
    private static final String CLIENTE_ID = "3f1c9a2e-0000-4000-8000-000000000001";

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRACAO_MS);

    private UserDetailsServiceImpl userDetailsService;
    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void preparar() {
        userDetailsService = mock(UserDetailsServiceImpl.class);
        filtro = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Reproduz exatamente o token da Lambda: HS256 (a lib de lá assina nesse algoritmo,
     * enquanto a jjwt daqui assinaria em HS512 por causa do tamanho da chave) e as claims
     * do contrato.
     */
    private String tokenDeCliente(long validadeSegundos, List<String> roles) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(CPF)
                .claim("tipo", "CLIENTE")
                .claim("clienteId", CLIENTE_ID)
                .claim("nome", "Fulano de Tal")
                .claim("roles", roles)
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + validadeSegundos * 1000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private void executar(String token) throws Exception {
        request.addHeader("Authorization", "Bearer " + token);
        filtro.doFilterInternal(request, response, chain);
    }

    private Authentication autenticacao() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("token de cliente autentica pelas claims, sem tocar na tabela de usuários")
    void deveAutenticarClienteSemConsultarUsuarios() throws Exception {
        executar(tokenDeCliente(3600, List.of("CLIENTE")));

        Authentication auth = autenticacao();
        assertNotNull(auth, "o cliente deveria ficar autenticado");
        assertEquals(CPF, auth.getPrincipal(), "o principal é o CPF do token");
        assertTrue(auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_CLIENTE"::equals),
                "deveria receber ROLE_CLIENTE");

        // O ponto central: um cliente não é usuário do sistema e não pode depender
        // de uma linha em "usuarios" para autenticar.
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("papéis declarados no token viram authorities com prefixo ROLE_")
    void deveMapearOsPapeisDoToken() throws Exception {
        executar(tokenDeCliente(3600, List.of("CLIENTE", "PREMIUM")));

        List<String> papeis = autenticacao().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        assertEquals(List.of("ROLE_CLIENTE", "ROLE_PREMIUM"), papeis);
    }

    @Test
    @DisplayName("token de cliente sem papéis recebe ROLE_CLIENTE por padrão")
    void deveAplicarPapelPadraoQuandoNaoHaRoles() throws Exception {
        executar(tokenDeCliente(3600, List.of()));

        assertEquals(List.of("ROLE_CLIENTE"), autenticacao().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
    }

    @Test
    @DisplayName("token de usuário continua sendo resolvido pela tabela de usuários")
    void deveManterOFluxoDeUsuario() throws Exception {
        UserDetails usuario = User.withUsername("admin@oficina.com")
                .password("irrelevante")
                .authorities("ROLE_ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin@oficina.com")).thenReturn(usuario);

        executar(jwtUtil.generateToken("admin@oficina.com"));

        Authentication auth = autenticacao();
        assertNotNull(auth, "o usuário deveria ficar autenticado");
        assertEquals(usuario, auth.getPrincipal());
        verify(userDetailsService).loadUserByUsername("admin@oficina.com");
    }

    @Test
    @DisplayName("token de cliente expirado não autentica")
    void deveRecusarTokenExpirado() throws Exception {
        executar(tokenDeCliente(-60, List.of("CLIENTE")));

        assertNull(autenticacao(), "token expirado não pode autenticar");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("token de cliente assinado com outro segredo não autentica")
    void deveRecusarAssinaturaInvalida() throws Exception {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "OUTRA_CHAVE_COMPLETAMENTE_DIFERENTE_E_LONGA_O_BASTANTE_2026____".getBytes(StandardCharsets.UTF_8));
        long agora = System.currentTimeMillis();
        String forjado = Jwts.builder()
                .subject(CPF)
                .claim("tipo", "CLIENTE")
                .expiration(new Date(agora + 3_600_000))
                .signWith(outraChave, Jwts.SIG.HS256)
                .compact();

        executar(forjado);

        assertNull(autenticacao(), "assinatura inválida não pode autenticar");
    }

    @Test
    @DisplayName("requisição sem header Authorization segue sem autenticação")
    void deveSeguirSemHeader() throws Exception {
        filtro.doFilterInternal(request, response, chain);

        assertNull(autenticacao());
        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
