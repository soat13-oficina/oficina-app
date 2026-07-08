package br.com.oficina.ordemservico.infrastructure.web.response;

import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.Funcionario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FuncionarioResponse", description = "Representação de um funcionário")
public record FuncionarioResponse(UUID id, String nome, String cpf) {
    @Override
    @Schema(description = "Identificador do funcionário", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    public UUID id() {
        return id;
    }

    @Override
    @Schema(description = "Nome do funcionário", example = "João da Silva")
    public String nome() {
        return nome;
    }

    @Override
    @Schema(description = "CPF do funcionário (somente dígitos)", example = "12345678909")
    public String cpf() {
        return cpf;
    }

    public static FuncionarioResponse from(Funcionario funcionario) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCpf());
    }
}
