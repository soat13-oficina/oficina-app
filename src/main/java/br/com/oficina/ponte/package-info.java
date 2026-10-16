/**
 * Ponte transitoria entre modulos que viram servicos diferentes (Fase 4, Passo 1 - ADR 0003).
 *
 * <p>{@code ordemservico}, {@code orcamento} e {@code pecainsumo} nao se importam (regra ArchUnit em
 * {@code ArquiteturaModulosTest}). Quando um precisa de uma consulta sincrona do outro, declara uma
 * porta no proprio pacote; o adaptador em processo mora aqui, o unico lugar que conhece os dois lados.
 *
 * <p>Cada classe daqui some na extracao do servico correspondente:
 * <ul>
 *   <li>{@link br.com.oficina.ponte.CatalogoDePecasDoEstoque}: no Billing vira cliente REST de
 *       {@code POST /internal/pecas-insumos/consulta}; na Execution, consulta local;</li>
 *   <li>{@link br.com.oficina.ponte.ReservasDaOrdemNoEstoque}: na Execution, execucao e estoque moram
 *       juntos e a baixa vira chamada local.</li>
 * </ul>
 * Comandos e eventos da Saga nunca passam por aqui: vao pela mensageria.
 */
package br.com.oficina.ponte;
