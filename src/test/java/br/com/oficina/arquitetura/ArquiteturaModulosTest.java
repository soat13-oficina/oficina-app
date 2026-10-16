package br.com.oficina.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fase 4, Passo 1: {@code ordemservico}, {@code orcamento} + {@code pagamento} e {@code pecainsumo}
 * viram tres microsservicos (OS, Billing e Execution - ADR 0003). Enquanto ainda moram no mesmo
 * processo, nao podem se conhecer por import: a conversa e por mensagens da Saga ou por portas, cujos
 * adaptadores em processo ficam no pacote {@code ponte} (o unico que enxerga mais de um lado).
 *
 * <p>Ate a 3a entrega do Passo 1 estas regras eram {@code FreezingArchRule}, com os acoplamentos
 * herdados registrados em {@code archunit_store}. O registro esvaziou: as regras agora sao estritas.
 */
@AnalyzeClasses(packages = "br.com.oficina", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    private static final String OS = "br.com.oficina.ordemservico..";
    private static final String ORCAMENTO = "br.com.oficina.orcamento..";
    private static final String PAGAMENTO = "br.com.oficina.pagamento..";
    private static final String ESTOQUE = "br.com.oficina.pecainsumo..";
    private static final String PONTE = "br.com.oficina.ponte..";

    @ArchTest
    static final ArchRule ordemServicoNaoConheceBillingNemEstoque = noClasses()
            .that().resideInAPackage(OS)
            .should().dependOnClassesThat().resideInAnyPackage(ORCAMENTO, PAGAMENTO, ESTOQUE, PONTE)
            .as("ordemservico (OS Service) nao depende de orcamento, pagamento, pecainsumo nem da ponte");

    @ArchTest
    static final ArchRule billingNaoConheceOrdemDeServicoNemEstoque = noClasses()
            .that().resideInAnyPackage(ORCAMENTO, PAGAMENTO)
            .should().dependOnClassesThat().resideInAnyPackage(OS, ESTOQUE, PONTE)
            .as("orcamento e pagamento (Billing) nao dependem de ordemservico, pecainsumo nem da ponte");

    @ArchTest
    static final ArchRule orcamentoNaoConhecePagamento = noClasses()
            .that().resideInAPackage(ORCAMENTO)
            .should().dependOnClassesThat().resideInAPackage(PAGAMENTO)
            .as("orcamento fala com pagamento so pela porta PagamentosDoOrcamento");

    @ArchTest
    static final ArchRule estoqueNaoConheceOrdemDeServicoNemBilling = noClasses()
            .that().resideInAPackage(ESTOQUE)
            .should().dependOnClassesThat().resideInAnyPackage(OS, ORCAMENTO, PAGAMENTO, PONTE)
            .as("pecainsumo (Execution) nao depende de ordemservico, orcamento, pagamento nem da ponte");

    @ArchTest
    static final ArchRule mensageriaNaoConheceNenhumModuloDeNegocio = noClasses()
            .that().resideInAnyPackage("br.com.oficina.common.application.mensageria..", "br.com.oficina.common.infrastructure.mensageria..")
            .should().dependOnClassesThat().resideInAnyPackage(OS, ORCAMENTO, PAGAMENTO, ESTOQUE, PONTE)
            .as("mensageria nao depende de modulos de negocio");
}
