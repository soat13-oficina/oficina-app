package br.com.oficina.auth.infrastructure.controller.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String senha;
}
