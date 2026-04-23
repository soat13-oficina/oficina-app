package br.com.oficina.ordemservico.application.service;

import java.text.Normalizer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.query.ListarFuncionariosQuery;
import br.com.oficina.ordemservico.application.usecase.ListarFuncionariosUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Service
public class ListarFuncionariosService implements ListarFuncionariosUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListarFuncionariosService.class);

    private final FuncionarioRepository funcionarioRepository;

    public ListarFuncionariosService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public List<Funcionario> listarFuncionarios(ListarFuncionariosQuery query) {
        log.info("Listando funcionarios. filtroNome={}", query.nome() != null && !query.nome().isBlank());

        List<Funcionario> funcionarios = funcionarioRepository.buscarTodos().stream()
                .filter(funcionario -> correspondeAoFiltro(funcionario, query.nome()))
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();

        log.info("Listagem de funcionarios concluida. quantidade={}", funcionarios.size());
        return funcionarios;
    }

    private boolean correspondeAoFiltro(Funcionario funcionario, String nome) {
        if (nome == null || nome.isBlank()) {
            return true;
        }
        return normalizarTexto(funcionario.getNome()).contains(normalizarTexto(nome));
    }

    private String normalizarTexto(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
