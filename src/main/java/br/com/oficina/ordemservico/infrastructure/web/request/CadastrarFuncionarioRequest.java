package br.com.oficina.ordemservico.infrastructure.web.request;

import br.com.oficina.ordemservico.application.command.CadastrarFuncionarioCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CadastrarFuncionarioRequest", description = "Dados necessários para cadastrar um funcionário")
public record CadastrarFuncionarioRequest(String nome, String cpf) {
    @Override
    @Schema(description = "Nome do funcionário", example = "João da Silva")
    public String nome() {
        return nome;
    }

    @Override
    @Schema(description = "CPF do funcionário (somente dígitos ou formatado). Opcional, mas quando informado deve conter 11 dígitos.", example = "12345678909")
    public String cpf() {
        return cpf;
    }

    public CadastrarFuncionarioCommand toCommand() {
        return new CadastrarFuncionarioCommand(nome, cpf);
    }
}
