package br.com.oficina.cliente.infrastructure.web.request;

import br.com.oficina.cliente.domain.model.TipoCliente;

public record AlterarClienteRequest(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
}
