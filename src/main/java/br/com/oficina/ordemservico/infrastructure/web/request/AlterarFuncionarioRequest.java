package br.com.oficina.ordemservico.infrastructure.web.request;

import java.util.UUID;

import br.com.oficina.ordemservico.application.command.AlterarFuncionarioCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlterarFuncionarioRequest", description = "Dados necessários para alterar um funcionário")
public record AlterarFuncionarioRequest(String nome, String cpf) {
    @Override
    @Schema(description = "Nome do funcionário", example = "João de Souza")
    public String nome() {
        return nome;
    }

    @Override
    @Schema(description = "CPF do funcionário (somente dígitos ou formatado). Opcional, mas quando informado deve conter 11 dígitos.", example = "12345678901")
    public String cpf() {
        return cpf;
    }

    public AlterarFuncionarioCommand toCommand(UUID funcionarioId) {
        return new AlterarFuncionarioCommand(funcionarioId, nome, cpf);
    }
}
