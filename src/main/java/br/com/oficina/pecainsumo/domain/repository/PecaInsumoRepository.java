package br.com.oficina.pecainsumo.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

public interface PecaInsumoRepository {
    void salvar(PecaInsumo pecaInsumo);

    Optional<PecaInsumo> buscarPorId(String id);

    List<PecaInsumo> buscarTodos();

    void excluirPorId(String id);
}
