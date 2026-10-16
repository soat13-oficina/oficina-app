package br.com.oficina.pecainsumo.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

public interface PecaInsumoRepository {
    void salvar(PecaInsumo pecaInsumo);

    Optional<PecaInsumo> buscarPorId(String id);

    /**
     * Le a peca travando a linha ate o fim da transacao: duas reservas simultaneas da mesma peca nao
     * podem ver o mesmo "disponivel". Sem banco (dubles de teste), igual a {@link #buscarPorId}.
     */
    default Optional<PecaInsumo> buscarParaAtualizar(String id) {
        return buscarPorId(id);
    }

    List<PecaInsumo> buscarTodos();

    void excluirPorId(String id);
}
