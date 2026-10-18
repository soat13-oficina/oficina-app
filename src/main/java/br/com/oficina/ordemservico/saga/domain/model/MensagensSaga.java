package br.com.oficina.ordemservico.saga.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tipos e payloads das mensagens que o orquestrador troca, como em {@code docs/contratos/mensagens.md}.
 *
 * <p>Copia propria do OS Service: Billing e Execution carregam, cada um, a sua versao do contrato - o
 * que os une e o JSON Schema, nao uma classe compartilhada. Dinheiro vai como texto
 * ({@code "530.00"}); campos opcionais nulos nao sao serializados.
 */
public final class MensagensSaga {

    // Comandos enviados pelo orquestrador - Billing
    public static final String GERAR_ORCAMENTO = "GerarOrcamento";
    public static final String REVERTER_APROVACAO = "ReverterAprovacao";
    public static final String CANCELAR_ORCAMENTO = "CancelarOrcamento";
    public static final String SOLICITAR_PAGAMENTO = "SolicitarPagamento";
    public static final String ESTORNAR_PAGAMENTO = "EstornarPagamento";

    // Comandos enviados pelo orquestrador - Execution
    public static final String ENFILEIRAR_DIAGNOSTICO = "EnfileirarDiagnostico";
    public static final String REABRIR_DIAGNOSTICO = "ReabrirDiagnostico";
    public static final String RESERVAR_PECAS = "ReservarPecas";
    public static final String ENFILEIRAR_EXECUCAO = "EnfileirarExecucao";
    public static final String LIBERAR_RESERVA = "LiberarReserva";
    public static final String CANCELAR_EXECUCAO = "CancelarExecucao";

    // Eventos recebidos - Billing
    public static final String ORCAMENTO_GERADO = "OrcamentoGerado";
    public static final String ORCAMENTO_FALHOU = "OrcamentoFalhou";
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_REJEITADO = "OrcamentoRejeitado";
    public static final String APROVACAO_REVERTIDA = "AprovacaoRevertida";
    public static final String ORCAMENTO_CANCELADO = "OrcamentoCancelado";
    public static final String PAGAMENTO_SOLICITADO = "PagamentoSolicitado";
    public static final String PAGAMENTO_CONFIRMADO = "PagamentoConfirmado";
    public static final String PAGAMENTO_RECUSADO = "PagamentoRecusado";
    public static final String PAGAMENTO_EXPIRADO = "PagamentoExpirado";
    public static final String PAGAMENTO_ESTORNADO = "PagamentoEstornado";
    public static final String ESTORNO_FALHOU = "EstornoFalhou";

    // Eventos recebidos - Execution
    public static final String DIAGNOSTICO_ENFILEIRADO = "DiagnosticoEnfileirado";
    public static final String DIAGNOSTICO_INICIADO = "DiagnosticoIniciado";
    public static final String DIAGNOSTICO_CONCLUIDO = "DiagnosticoConcluido";
    public static final String DIAGNOSTICO_REABERTO = "DiagnosticoReaberto";
    public static final String PECAS_RESERVADAS = "PecasReservadas";
    public static final String RESERVA_FALHOU = "ReservaFalhou";
    public static final String EXECUCAO_ENFILEIRADA = "ExecucaoEnfileirada";
    public static final String EXECUCAO_INICIADA = "ExecucaoIniciada";
    public static final String EXECUCAO_CONCLUIDA = "ExecucaoConcluida";
    public static final String EXECUCAO_FALHOU = "ExecucaoFalhou";
    public static final String RESERVA_LIBERADA = "ReservaLiberada";
    public static final String EXECUCAO_CANCELADA = "ExecucaoCancelada";

    /** Todos os eventos que o orquestrador consome em {@code os-saga-replies}. */
    public static final List<String> EVENTOS = List.of(
            ORCAMENTO_GERADO, ORCAMENTO_FALHOU, ORCAMENTO_APROVADO, ORCAMENTO_REJEITADO, APROVACAO_REVERTIDA,
            ORCAMENTO_CANCELADO, PAGAMENTO_SOLICITADO, PAGAMENTO_CONFIRMADO, PAGAMENTO_RECUSADO, PAGAMENTO_EXPIRADO,
            PAGAMENTO_ESTORNADO, ESTORNO_FALHOU, DIAGNOSTICO_ENFILEIRADO, DIAGNOSTICO_INICIADO, DIAGNOSTICO_CONCLUIDO,
            DIAGNOSTICO_REABERTO, PECAS_RESERVADAS, RESERVA_FALHOU, EXECUCAO_ENFILEIRADA, EXECUCAO_INICIADA,
            EXECUCAO_CONCLUIDA, EXECUCAO_FALHOU, RESERVA_LIBERADA, EXECUCAO_CANCELADA);

    private MensagensSaga() {
    }

    // ------------------------------------------------------------------ estruturas comuns

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cliente(UUID id, String nome, String documento, String tipo, String email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Veiculo(String placa, String marca, String modelo, Integer ano) {
    }

    public record Funcionario(UUID id, String nome) {
    }

    /** Funcionario que executou a acao (claims do JWT na Execution). */
    public record Mecanico(String id, String nome) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Servico(String descricao, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorMaoDeObra) {
    }

    public record ItemPeca(String pecaInsumoId, int quantidade) {
    }

    public record PecaFaltante(String pecaInsumoId, int solicitada, int disponivel) {
    }

    // ------------------------------------------------------------------ comandos

    /**
     * Snapshot do diagnostico para o Billing gerar o orcamento. A mao de obra vem por servico, no
     * {@code DiagnosticoConcluido}; {@code valorMaoDeObra}, {@code desconto}, {@code validade} e
     * {@code observacoes} sao opcionais no contrato e o OS nao os informa mais (o Billing usa a soma da mao
     * de obra dos servicos, desconto zero e a validade padrao).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GerarOrcamento(
            String numeroOrdemServico,
            Cliente cliente,
            Veiculo veiculo,
            Funcionario funcionario,
            String descricaoDiagnostico,
            List<Servico> servicos,
            List<ItemPeca> pecas,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorMaoDeObra,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal desconto,
            Instant validade,
            String observacoes) {
    }

    /** Abertura da OS: entra na fila de diagnostico da Execution, com a previsao do atendente como ponto de partida. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EnfileirarDiagnostico(
            String numeroOrdemServico, Veiculo veiculo, List<Servico> servicosPrevistos, List<ItemPeca> pecasPrevistas, Instant abertaEm) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReabrirDiagnostico(String motivo, List<String> pecasInvalidas) {
    }

    public record ReservarPecas(UUID orcamentoId, List<ItemPeca> itens) {
    }

    public record EnfileirarExecucao(UUID orcamentoId) {
    }

    public record CancelarExecucao(String motivo) {
    }

    public record LiberarReserva(String motivo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReverterAprovacao(UUID orcamentoId, String motivo, List<PecaFaltante> pecasFaltantes) {
    }

    public record CancelarOrcamento(UUID orcamentoId, String motivo) {
    }

    public record SolicitarPagamento(UUID orcamentoId, Instant expiraEm) {
    }

    public record EstornarPagamento(UUID orcamentoId, UUID pagamentoId, String motivo) {
    }

    // ------------------------------------------------------------------ eventos (so o que o OS le)

    public record OrcamentoGerado(UUID orcamentoId, String numeroOrcamento, BigDecimal valorTotal, Instant validade) {
    }

    public record OrcamentoFalhou(String motivo, List<String> pecasInvalidas) {
    }

    public record OrcamentoDecidido(UUID orcamentoId, String canal, Instant decididoEm) {
    }

    public record OrcamentoCancelado(UUID orcamentoId, boolean pagamentoCancelado) {
    }

    public record DiagnosticoConcluido(
            Mecanico mecanico, String descricaoDiagnostico, List<Servico> servicos, List<ItemPeca> pecas, Instant concluidoEm) {
    }

    public record ExecucaoFalhou(Mecanico mecanico, String motivo, Instant abortadaEm) {
    }

    public record ReservaFalhou(UUID orcamentoId, List<PecaFaltante> faltantes) {
    }

    public record PagamentoSolicitado(UUID pagamentoId, BigDecimal valor, String linkPagamento, Instant expiraEm) {
    }

    public record PagamentoConfirmado(UUID pagamentoId, String mpPaymentId, BigDecimal valor, Instant aprovadoEm) {
    }

    public record PagamentoRecusado(UUID pagamentoId, String mpPaymentId, String motivo) {
    }

    public record EstornoFalhou(UUID pagamentoId, String motivo) {
    }
}
