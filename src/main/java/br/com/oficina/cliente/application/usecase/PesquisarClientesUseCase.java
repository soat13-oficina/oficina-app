package br.com.oficina.cliente.application.usecase;

import java.util.List;

import br.com.oficina.cliente.application.query.PesquisarClientesQuery;
import br.com.oficina.cliente.domain.model.Cliente;

public interface PesquisarClientesUseCase {
    List<Cliente> pesquisarClientes(PesquisarClientesQuery query);
}
