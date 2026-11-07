package br.com.oficina.support.saga;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.ordemservico.application.port.CatalogoPecas;

/**
 * Cadastro e estoque de pecas de mentira: o dono dos dois e o {@code oficina-execution-service}. Serve a
 * tres papeis nos testes do OS - a porta {@link CatalogoPecas} (conferencia na abertura da OS), o preco que
 * o {@link BillingSimulado} consulta (a consulta REST interna do Billing) e o estoque que o
 * {@link ExecutionSimulado} reserva, libera e baixa. As regras reais do estoque tem os testes do
 * proprio servico.
 */
public class CatalogoSimulado implements CatalogoPecas {

    public static final class Peca {
        private final String id;
        private final String descricao;
        private final BigDecimal preco;
        private volatile int estoque;
        private volatile int reservada;

        Peca(String id, String descricao, BigDecimal preco, int estoque) {
            this.id = id;
            this.descricao = descricao;
            this.preco = preco;
            this.estoque = estoque;
        }

        public String getId() {
            return id;
        }

        public String getDescricao() {
            return descricao;
        }

        public BigDecimal getPreco() {
            return preco;
        }

        public int getEstoque() {
            return estoque;
        }

        public int getReservada() {
            return reservada;
        }

        public int getDisponivel() {
            return estoque - reservada;
        }
    }

    private final Map<String, Peca> pecas = new ConcurrentHashMap<>();

    public void limpar() {
        pecas.clear();
    }

    /** Peca de R$ 80,00 com {@code estoque} unidades; devolve o id. */
    public String cadastrar(String descricao, int estoque) {
        String id = UUID.randomUUID().toString();
        pecas.put(id, new Peca(id, descricao, new BigDecimal("80.00"), estoque));
        return id;
    }

    public Optional<Peca> buscar(String id) {
        return Optional.ofNullable(pecas.get(id));
    }

    public Peca peca(String id) {
        return buscar(id).orElseThrow(() -> new IllegalArgumentException("Peca simulada nao existe: " + id));
    }

    /** O estoque cai para {@code estoque} unidades, sem reserva. */
    public void definirEstoque(String id, int estoque) {
        Peca antiga = peca(id);
        pecas.put(id, new Peca(id, antiga.descricao, antiga.preco, estoque));
    }

    public void remover(String id) {
        pecas.remove(id);
    }

    void reservar(String id, int quantidade) {
        peca(id).reservada += quantidade;
    }

    void liberar(String id, int quantidade) {
        Peca peca = peca(id);
        peca.reservada = Math.max(0, peca.reservada - quantidade);
    }

    void consumir(String id, int quantidade) {
        Peca peca = peca(id);
        peca.reservada = Math.max(0, peca.reservada - quantidade);
        peca.estoque = Math.max(0, peca.estoque - quantidade);
    }

    @Override
    public Set<String> inexistentes(Collection<String> pecaInsumoIds) {
        Set<String> inexistentes = new LinkedHashSet<>();
        for (String id : new LinkedHashSet<>(pecaInsumoIds)) {
            if (!pecas.containsKey(id)) {
                inexistentes.add(id);
            }
        }
        return inexistentes;
    }
}
