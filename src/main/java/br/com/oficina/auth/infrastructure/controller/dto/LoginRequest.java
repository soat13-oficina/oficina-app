package br.com.oficina.auth.infrastructure.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "O email e obrigatorio.")
    @Email(message = "O email deve ter um formato valido.")
    private String email;

    @NotBlank(message = "A senha e obrigatoria.")
    private String senha;
}
