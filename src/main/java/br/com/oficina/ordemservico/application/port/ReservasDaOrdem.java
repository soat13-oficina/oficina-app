package br.com.oficina.ordemservico.application.port;

import java.util.UUID;

/**
 * Reservas de pecas que a Saga fez para a OS (passo {@code ReservarPecas}). Na conclusao do servico
 * as pecas reservadas sao baixadas do estoque.
 *
 * <p>No monolito, o adaptador fica em {@code br.com.oficina.ponte}. Na Execution Service, execucao e
 * estoque moram juntos e isto vira chamada local (o evento {@code ExecucaoConcluida} ja sai com as
 * pecas consumidas).
 */
public interface ReservasDaOrdem {

    /** Baixa do estoque o que esta reservado para a OS; devolve quantas pecas (itens) foram consumidas. */
    int consumir(UUID ordemDeServicoId);
}
