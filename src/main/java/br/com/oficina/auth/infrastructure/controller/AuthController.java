package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.domain.model.Usuario;
import br.com.oficina.auth.infrastructure.controller.dto.LoginRequest;
import br.com.oficina.auth.infrastructure.controller.dto.LoginResponse;
import br.com.oficina.auth.infrastructure.repository.UsuarioRepository;
import br.com.oficina.auth.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
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

//TODO refatorar controler com boas praticas
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwagger {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha()));

            String token = jwtUtil.generateToken(authentication.getName());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRequest request) {
        try {
            // 1. Verifica se já existe
            if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(409).build();
            }

            // 2. Cria usuário
            Usuario user = new Usuario();
            user.setEmail(request.getEmail());
            user.setSenha(encoder.encode(request.getSenha()));

            usuarioRepository.save(user);

            String token = jwtUtil.generateToken(user.getEmail());

            return ResponseEntity.ok(new LoginResponse(token));

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
