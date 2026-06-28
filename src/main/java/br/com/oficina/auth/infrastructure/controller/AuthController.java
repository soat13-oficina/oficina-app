package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.domain.model.Usuario;
import br.com.oficina.auth.infrastructure.controller.dto.LoginRequest;
import br.com.oficina.auth.infrastructure.controller.dto.LoginResponse;
import br.com.oficina.auth.infrastructure.repository.UsuarioRepository;
import br.com.oficina.auth.infrastructure.security.JwtUtil;
import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwagger {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login iniciado. email={}", request.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha()));

            String token = jwtUtil.generateToken(authentication.getName());
            log.info("Login concluido com sucesso. email={}", request.getEmail());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            log.warn("Login com credenciais invalidas. email={}", request.getEmail());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody LoginRequest request) {
        log.info("Registro de usuario iniciado. email={}", request.getEmail());

        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registro rejeitado: email ja cadastrado. email={}", request.getEmail());
            throw new ConflitoDeRecursoException("Email ja cadastrado.");
        }

        Usuario user = new Usuario();
        user.setEmail(request.getEmail());
        user.setSenha(encoder.encode(request.getSenha()));

        usuarioRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("Registro de usuario concluido com sucesso. email={}", request.getEmail());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
