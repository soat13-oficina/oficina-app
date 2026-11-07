package br.com.oficina.ordemservico.infrastructure.web;

import java.util.List;

import br.com.oficina.ordemservico.infrastructure.web.request.AlterarOrdemDeServicoRequest;
import br.com.oficina.ordemservico.infrastructure.web.request.CriarOrdemDeServicoRequest;
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

    @Operation(summary = "Entregar ordem de serviço", description = "Conclui a entrega de uma ordem de serviço finalizada ao cliente, alterando o status para ENTREGUE e registrando o timestamp de entrega.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço entregue com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada", content = @Content)
    })
    ResponseEntity<Void> entregarAoCliente(String numeroOrdemServico);
}
