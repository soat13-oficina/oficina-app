package br.com.oficina.ordemservico.infrastructure.web;

import java.util.List;

import br.com.oficina.ordemservico.infrastructure.web.request.AbortarExecucaoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.AlterarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.ConcluirDiagnosticoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.infrastructure.web.response.AberturaOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.AcompanhamentoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.HistoricoOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.OrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.StatusOrdemDeServicoResponse;
import br.com.oficina.ordemservico.infrastructure.web.response.TempoMedioExecucaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Ordens de Servico", description = "Operações de abertura, acompanhamento, execução, entrega e métricas de ordens de serviço")
@SecurityRequirement(name = "bearerAuth")
public interface OrdemDeServicoControllerSwagger {

    @Operation(
            summary = "Criar ordem de serviço",
            description = "Abre uma nova ordem de serviço vinculando cliente, funcionário responsável e veículo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem de serviço criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido ou dados inconsistentes", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente, funcionário, veículo ou peça não encontrado", content = @Content)
    })
    ResponseEntity<AberturaOrdemDeServicoResponse> criar(CriarOrdemDeServicoRequest request);

    @Operation(
            summary = "Alterar ordem de serviço",
            description = "Atualiza cliente e veículo de uma ordem aberta, mantendo o funcionário criador original.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido ou regra de negócio violada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem, cliente, funcionário ou veículo não encontrado", content = @Content)
    })
    ResponseEntity<Void> alterar(String numeroOrdemServico, AlterarOrdemDeServicoRequest request);

    @Operation(summary = "Excluir ordem de serviço", description = "Remove uma ordem de serviço pelo número informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> excluir(String numeroOrdemServico);

    @Operation(
            summary = "Consultar ordens de serviço (fila priorizada)",
            description = "Retorna ordens de serviço ativas ordenadas por prioridade de situação "
                    + "(Execução > Aguardando Aprovação/Pagamento > Diagnóstico > Recebida) e, dentro de cada situação, "
                    + "pela antiguidade (iniciadaEm asc). Ordens Finalizadas, Entregues e Canceladas são excluídas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordens de serviço retornadas com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrdemDeServicoResponse.class))))
    })
    ResponseEntity<List<OrdemDeServicoResponse>> consultar(
            @Parameter(description = "Número da ordem de serviço para filtro exato.") String numeroOrdemServico,
            @Parameter(description = "Nome completo ou parcial do cliente vinculado.") String nomeCliente,
            @Parameter(description = "Placa do veículo vinculada à ordem de serviço.") String placaVeiculo,
            @Parameter(description = "CPF ou CNPJ do cliente vinculado.") String documentoCliente);

    @Operation(
            summary = "Consultar tempo médio de execução",
            description = "Retorna a média geral de execução calculada a partir de iniciadaEm e finalizadaEm das ordens com execução concluída, incluindo ordens já entregues ao cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métrica retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = TempoMedioExecucaoResponse.class)))
    })
    ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao();

    @Operation(
            summary = "Acompanhar ordem de serviço",
            description = "Permite ao cliente consultar o andamento da ordem de serviço informando o número da ordem e o documento do cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acompanhamento retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = AcompanhamentoOrdemDeServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<AcompanhamentoOrdemDeServicoResponse> acompanhar(
            String numeroOrdemServico,
            @Parameter(description = "CPF ou CNPJ do cliente vinculado à ordem de serviço.") String documentoCliente);

    @Operation(summary = "Consultar status da ordem de serviço",
            description = "Retorna a situação de negócio canônica atual da ordem de serviço "
                    + "(Recebida, Diagnóstico, Aguardando Aprovação, Aguardando Pagamento, Execução, Finalizada, Entregue ou Cancelada), "
                    + "com motivo de encerramento quando aplicável.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = StatusOrdemDeServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<StatusOrdemDeServicoResponse> consultarStatus(String numeroOrdemServico);

    @Operation(summary = "Consultar histórico da ordem de serviço",
            description = "Retorna a situação atual e todas as mudanças de status da ordem de serviço, "
                    + "da mais antiga para a mais recente, com a origem de cada mudança "
                    + "(API, SAGA, TIMEOUT ou MIGRACAO) e a saga responsável, quando houver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = HistoricoOrdemDeServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<HistoricoOrdemDeServicoResponse> consultarHistorico(String numeroOrdemServico);

    @Operation(summary = "Iniciar diagnóstico", description = "Inicia o diagnóstico de uma ordem de serviço aberta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diagnóstico iniciado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> iniciarDiagnostico(String numeroOrdemServico);

    @Operation(summary = "Concluir diagnóstico",
            description = "Conclui o diagnóstico de uma OS em andamento. Pode receber, no corpo, as peças a usar e a descrição do serviço (fonte das peças do orçamento).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diagnóstico concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço ou peça não encontrada", content = @Content)
    })
    ResponseEntity<Void> concluirDiagnostico(String numeroOrdemServico, ConcluirDiagnosticoRequest request);

    @Operation(
            summary = "Enviar diagnóstico para orçamento",
            description = "Pede ao Billing, pela Saga, o orçamento do diagnóstico concluído (comando GerarOrcamento). "
                    + "Assíncrono: a resposta 202 só confirma o pedido. Quando o orçamento é gerado (alguns segundos), "
                    + "a OS vai para 'Aguardando Aprovação' e o cliente recebe o e-mail; se uma peça do diagnóstico não "
                    + "existir mais, o diagnóstico é reaberto. Acompanhe por GET /ordens-servico/{n} ou GET /sagas/{ordemServicoId}. "
                    + "Valor da mão de obra obrigatório; validade opcional (padrão do Billing: 7 dias).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pedido de orçamento aceito; a OS muda quando o Billing responder"),
            @ApiResponse(responseCode = "400", description = "Diagnóstico não concluído, dado financeiro inválido ou orçamento já pedido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> enviarDiagnosticoParaOrcamento(
            String numeroOrdemServico,
            EnviarDiagnosticoParaOrcamentoRequest request);

    @Operation(
            summary = "Iniciar execução",
            description = "O mecânico começa o serviço de uma OS paga (status AGUARDANDO_EXECUCAO, situação 'Execução'). "
                    + "A OS chega lá pela Saga: orçamento aprovado, peças reservadas e pagamento confirmado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Execução iniciada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> iniciarExecucao(String numeroOrdemServico);

    @Operation(
            summary = "Concluir serviço",
            description = "Move a ordem de serviço de 'Execução' para 'Finalizada', registrando o motivo SERVICO_CONCLUIDO "
                    + "e baixando do estoque as peças reservadas para ela.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> concluirServico(String numeroOrdemServico);

    @Operation(
            summary = "Abortar execução",
            description = "O mecânico não consegue concluir o serviço. A OS vai para 'Cancelada' (motivo FALHA_EXECUCAO) e a Saga "
                    + "compensa: estorna o pagamento e libera as peças reservadas (eventos PagamentoEstornado e ReservaLiberada).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Execução abortada; compensações enviadas"),
            @ApiResponse(responseCode = "400", description = "OS não está em execução ou motivo ausente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> abortarExecucao(String numeroOrdemServico, AbortarExecucaoRequest request);

    @Operation(summary = "Entregar ordem de serviço", description = "Conclui a entrega de uma ordem de serviço finalizada ao cliente, alterando o status para ENTREGUE e registrando o timestamp de entrega.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço entregue com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> entregarAoCliente(String numeroOrdemServico);
}
