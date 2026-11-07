package br.com.oficina.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fase 4: o monolito virou o OS Service. Orcamento e pagamento sairam para o Billing Service (Passo 3);
 * estoque, diagnostico e execucao, para o Execution Service (Passo 4). Nenhum dos dois pode voltar para ca:
 * o OS conversa com eles so por mensagens da Saga e por REST interno (adaptador em
 * {@code ordemservico.infrastructure}), nunca por import.
 *
 * <p>Ate a 3a entrega do Passo 1 estas regras eram {@code FreezingArchRule}, com os acoplamentos
 * herdados registrados em {@code archunit_store}. O registro esvaziou: as regras sao estritas.
 */
@AnalyzeClasses(packages = "br.com.oficina", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    private static final String ORCAMENTO = "br.com.oficina.orcamento..";
    private static final String PAGAMENTO = "br.com.oficina.pagamento..";
    private static final String ESTOQUE = "br.com.oficina.pecainsumo..";
    private static final String PONTE = "br.com.oficina.ponte..";

    @ArchTest
    static final ArchRule billingNaoVoltaParaOOs = noClasses()
            .should().resideInAnyPackage(ORCAMENTO, PAGAMENTO)
            .as("orcamento e pagamento moram no oficina-billing-service");

    @ArchTest
    static final ArchRule executionNaoVoltaParaOOs = noClasses()
            .should().resideInAnyPackage(ESTOQUE, PONTE)
            .as("estoque de pecas, diagnostico e execucao moram no oficina-execution-service");

    @ArchTest
    static final ArchRule ordemServicoNaoUsaClassesDeBillingNemExecution = noClasses()
            .that().resideInAPackage("br.com.oficina.ordemservico..")
            .should().dependOnClassesThat().resideInAnyPackage(ORCAMENTO, PAGAMENTO, ESTOQUE, PONTE)
            .as("ordemservico (OS Service) so conversa com Billing e Execution por mensagens e REST");

    @ArchTest
    static final ArchRule mensageriaNaoConheceNenhumModuloDeNegocio = noClasses()
            .that().resideInAnyPackage("br.com.oficina.common.application.mensageria..", "br.com.oficina.common.infrastructure.mensageria..")
            .should().dependOnClassesThat().resideInAnyPackage("br.com.oficina.ordemservico..", ORCAMENTO, PAGAMENTO, ESTOQUE, PONTE)
            .as("mensageria nao depende de modulos de negocio");
}
