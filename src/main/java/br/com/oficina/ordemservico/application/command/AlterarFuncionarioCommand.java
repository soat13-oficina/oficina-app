package br.com.oficina.ordemservico.application.command;

import java.util.UUID;

public record AlterarFuncionarioCommand(UUID funcionarioId, String nome, String cpf) {
}
