package br.com.oficina.ordemservico.application.port;

import java.util.Collection;
import java.util.Set;

/**
 * Consulta ao cadastro de pecas, que e da Execution (ADR 0003). Usada para validar as pecas
 * informadas na abertura e no fechamento do diagnostico.
 *
 * <p>No monolito, o adaptador fica em {@code br.com.oficina.ponte}. Quando o diagnostico for para a
 * Execution a consulta vira local la; na abertura da OS, REST interno.
 */
public interface CatalogoPecas {

    /** Ids informados que nao existem no cadastro (vazio = todos existem). */
    Set<String> inexistentes(Collection<String> pecaInsumoIds);
}
