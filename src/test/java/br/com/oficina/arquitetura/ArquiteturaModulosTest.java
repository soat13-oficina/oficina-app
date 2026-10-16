package br.com.oficina.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fase 4: {@code ordemservico} e {@code pecainsumo} viram microsservicos diferentes (OS e Execution -
 * ADR 0003); orcamento e pagamento ja sairam para o Billing Service (Passo 3) e nao podem voltar.
 * Enquanto OS e estoque moram no mesmo processo, nao se conhecem por import: a conversa e por
 * mensagens da Saga ou por portas, cujos adaptadores em processo ficam no pacote {@code ponte} (o
 * unico que enxerga os dois lados).
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
    static final ArchRule billingNaoVoltaParaOOs = noClasses()
            .should().resideInAnyPackage(ORCAMENTO, PAGAMENTO)
            .as("orcamento e pagamento moram no oficina-billing-service");

    @ArchTest
    static final ArchRule ordemServicoNaoConheceEstoque = noClasses()
            .that().resideInAPackage(OS)
            .should().dependOnClassesThat().resideInAnyPackage(ESTOQUE, PONTE)
            .as("ordemservico (OS Service) nao depende de pecainsumo nem da ponte");

    @ArchTest
    static final ArchRule estoqueNaoConheceOrdemDeServico = noClasses()
            .that().resideInAPackage(ESTOQUE)
            .should().dependOnClassesThat().resideInAnyPackage(OS, PONTE)
            .as("pecainsumo (Execution) nao depende de ordemservico nem da ponte");

    @ArchTest
    static final ArchRule mensageriaNaoConheceNenhumModuloDeNegocio = noClasses()
            .that().resideInAnyPackage("br.com.oficina.common.application.mensageria..", "br.com.oficina.common.infrastructure.mensageria..")
            .should().dependOnClassesThat().resideInAnyPackage(OS, ESTOQUE, PONTE)
            .as("mensageria nao depende de modulos de negocio");
}
