package br.com.oficina.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Fase 4, Passo 1: {@code ordemservico}, {@code orcamento} e {@code pecainsumo} viram tres
 * microsservicos (OS, Billing e Execution - ADR 0003). Enquanto ainda moram no mesmo processo, nao
 * podem se conhecer por import: a conversa e por mensagens da Saga ou por portas.
 *
 * <p>As regras sao {@link FreezingArchRule}: os acoplamentos que JA existiam quando a regra nasceu
 * ficam registrados em {@code src/test/resources/archunit_store} e sao tolerados; qualquer acoplamento
 * NOVO reprova o build. Cada acoplamento removido sai do registro sozinho - quando o registro de uma
 * regra esvaziar, o desacoplamento daquele modulo terminou (criterio de pronto do Passo 1).
 */
@AnalyzeClasses(packages = "br.com.oficina", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    @ArchTest
    static final ArchRule ordemServicoNaoConheceOrcamentoNemEstoque = FreezingArchRule.freeze(noClasses()
            .that().resideInAPackage("br.com.oficina.ordemservico..")
            .should().dependOnClassesThat().resideInAnyPackage("br.com.oficina.orcamento..", "br.com.oficina.pecainsumo..")
            .as("ordemservico nao depende de orcamento nem de pecainsumo"));

    @ArchTest
    static final ArchRule orcamentoNaoConheceOrdemDeServicoNemEstoque = FreezingArchRule.freeze(noClasses()
            .that().resideInAPackage("br.com.oficina.orcamento..")
            .should().dependOnClassesThat().resideInAnyPackage("br.com.oficina.ordemservico..", "br.com.oficina.pecainsumo..")
            .as("orcamento nao depende de ordemservico nem de pecainsumo"));

    @ArchTest
    static final ArchRule estoqueNaoConheceOrdemDeServicoNemOrcamento = FreezingArchRule.freeze(noClasses()
            .that().resideInAPackage("br.com.oficina.pecainsumo..")
            .should().dependOnClassesThat().resideInAnyPackage("br.com.oficina.ordemservico..", "br.com.oficina.orcamento..")
            .as("pecainsumo nao depende de ordemservico nem de orcamento"));

    @ArchTest
    static final ArchRule mensageriaNaoConheceNenhumModuloDeNegocio = noClasses()
            .that().resideInAnyPackage("br.com.oficina.common.application.mensageria..", "br.com.oficina.common.infrastructure.mensageria..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.oficina.ordemservico..", "br.com.oficina.orcamento..", "br.com.oficina.pecainsumo..")
            .as("mensageria nao depende de modulos de negocio");
}
