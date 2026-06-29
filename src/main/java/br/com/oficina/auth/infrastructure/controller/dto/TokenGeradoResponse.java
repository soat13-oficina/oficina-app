package br.com.oficina.auth.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.auth.infrastructure.security.TokenIntegracaoService.TokenGerado;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenGeradoResponse", description = "Token de integração recém-gerado (segredo exibido uma única vez)")
public record TokenGeradoResponse(
        UUID id,
        String rotulo,
        @Schema(description = "Segredo do token em claro — exibido apenas nesta resposta", example = "oki_xxxxx")
        String token,
        LocalDateTime criadoEm,
        String criadoPor) {

    public static TokenGeradoResponse from(TokenGerado gerado) {
        return new TokenGeradoResponse(
                gerado.token().getId(),
                gerado.token().getRotulo(),
                gerado.tokenEmClaro(),
                gerado.token().getCriadoEm(),
                gerado.token().getCriadoPor());
    }
}
