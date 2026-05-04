package br.com.oficina.ordemservico.application.command;

import java.util.UUID;

public record ExcluirFuncionarioCommand(UUID funcionarioId) {
}
