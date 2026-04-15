package br.com.oficina.cliente.application.service;

import java.text.Normalizer;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.query.PesquisarClientesQuery;
import br.com.oficina.cliente.application.usecase.PesquisarClientesUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class PesquisarClientesService implements PesquisarClientesUseCase {
    private final ClienteRepository clienteRepository;

    public PesquisarClientesService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public List<Cliente> pesquisarClientes(PesquisarClientesQuery query) {
        String termo = query.termo();
        if (termo == null || termo.isBlank()) {
            return clienteRepository.buscarTodos().stream()
                    .sorted((cliente1, cliente2) -> cliente1.getNome().compareToIgnoreCase(cliente2.getNome()))
                    .toList();
        }

        String termoNormalizado = normalizarTexto(termo);
        String termoNumerico = manterSomenteDigitos(termo);

        return clienteRepository.buscarTodos().stream()
                .filter(cliente -> correspondeAoCpf(cliente, termo, termoNumerico)
                        || correspondeAoNome(cliente, termoNormalizado))
                .sorted((cliente1, cliente2) -> cliente1.getNome().compareToIgnoreCase(cliente2.getNome()))
                .toList();
    }

    private boolean correspondeAoCpf(Cliente cliente, String termoOriginal, String termoNumerico) {
        if (cliente.getCpfOuCnpj() == null || cliente.getCpfOuCnpj().isBlank()) {
            return false;
        }

        return cliente.getCpfOuCnpj().equalsIgnoreCase(termoOriginal.trim())
                || (!termoNumerico.isBlank() && manterSomenteDigitos(cliente.getCpfOuCnpj()).equals(termoNumerico));
    }

    private boolean correspondeAoNome(Cliente cliente, String termoNormalizado) {
        return normalizarTexto(cliente.getNome()).contains(termoNormalizado);
    }

    private String normalizarTexto(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private String manterSomenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }
}
