package br.com.oficina.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Autentica a requisição a partir do JWT do header {@code Authorization}.
 *
 * <p>Aceita tokens de duas origens:
 *
 * <ul>
 *   <li><b>Usuário</b> — emitido por {@code POST /api/auth/login} com e-mail e senha.
 *       O subject é o e-mail e a identidade é carregada da tabela {@code usuarios}.</li>
 *   <li><b>Cliente</b> — emitido pela Function Serverless de autenticação
 *       (repositório {@code oficina-lambda-auth}) a partir do CPF, e reconhecido pela
 *       claim {@code tipo=CLIENTE}. Aqui a identidade vem das próprias claims:
 *       um cliente não tem — e não deve precisar ter — linha em {@code usuarios}.</li>
 * </ul>
 *
 * <p>Os dois tokens são assinados com o mesmo segredo HMAC ({@code JWT_SECRET}), o que
 * permite a esta aplicação validar o que a Lambda emitiu sem chamada de rede.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String PREFIXO_ROLE = "ROLE_";
    private static final List<SimpleGrantedAuthority> AUTORIDADES_PADRAO_CLIENTE =
            List.of(new SimpleGrantedAuthority(PREFIXO_ROLE + TIPO_CLIENTE));

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(PREFIXO_BEARER.length());

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // A leitura das claims já valida assinatura e expiração: a jjwt lança
                // se qualquer uma das duas falhar, e caímos no catch abaixo.
                if (TIPO_CLIENTE.equals(jwtUtil.extractTipo(jwt))) {
                    autenticarCliente(jwt, request);
                } else {
                    autenticarUsuario(jwt, request);
                }
            }
        } catch (Exception e) {
            // Se o token for inválido, não fazemos nada e a requisição continuará como não autenticada.
        }

        filterChain.doFilter(request, response);
    }

    /** Fluxo original: o subject é um e-mail e a identidade vem da tabela {@code usuarios}. */
    private void autenticarUsuario(String jwt, HttpServletRequest request) {
        String userEmail = jwtUtil.extractEmail(jwt);
        if (userEmail == null) {
            return;
        }

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

        if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
            registrar(userDetails, userDetails.getAuthorities(), request);
        }
    }

    /**
     * Cliente autenticado por CPF: a identidade é o próprio token.
     *
     * <p>Nenhuma consulta a {@code usuarios} acontece aqui. A Lambda já verificou, contra
     * o banco, que o cliente existe e está ativo antes de assinar — repetir a checagem
     * exigiria que todo cliente também fosse usuário do sistema, que é justamente o
     * acoplamento que o fluxo por CPF evita.
     */
    private void autenticarCliente(String jwt, HttpServletRequest request) {
        String cpf = jwtUtil.extractSubject(jwt);
        if (cpf == null || cpf.isBlank()) {
            return;
        }

        List<SimpleGrantedAuthority> autoridades = jwtUtil.extractRoles(jwt).stream()
                .map(papel -> new SimpleGrantedAuthority(PREFIXO_ROLE + papel))
                .toList();

        registrar(cpf, autoridades.isEmpty() ? AUTORIDADES_PADRAO_CLIENTE : autoridades, request);
    }

    private void registrar(Object principal,
                           Collection<? extends GrantedAuthority> autoridades,
                           HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, autoridades);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
