/**
 * Ponte transitoria entre modulos que viram servicos diferentes (Fase 4 - ADR 0003).
 *
 * <p>{@code ordemservico} e {@code pecainsumo} nao se importam (regra ArchUnit em
 * {@code ArquiteturaModulosTest}). Quando um precisa de uma consulta sincrona do outro, declara uma
 * porta no proprio pacote; o adaptador em processo mora aqui, o unico lugar que conhece os dois lados.
 * O orcamento e o pagamento ja sairam para o Billing Service (Passo 3).
 *
 * <p>Cada classe daqui some na extracao da Execution (Passo 4):
 * <ul>
 *   <li>{@link br.com.oficina.ponte.CatalogoDePecasDoEstoque}: na Execution, consulta local; na
 *       abertura da OS, REST interno;</li>
 *   <li>{@link br.com.oficina.ponte.ReservasDaOrdemNoEstoque}: na Execution, execucao e estoque moram
 *       juntos e a baixa vira chamada local.</li>
 * </ul>
 * Comandos e eventos da Saga nunca passam por aqui: vao pela mensageria.
 */
package br.com.oficina.ponte;
