package br.com.oficina.ordemservico.saga.infrastructure.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.oficina.ordemservico.saga.application.ConsultarSagaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/sagas")
@Tag(name = "Saga", description = "Acompanhamento da Saga que coordena o ciclo de vida da ordem de serviço (Fase 4)")
public class SagaController {

    private final ConsultarSagaUseCase consultarSaga;

    public SagaController(ConsultarSagaUseCase consultarSaga) {
        this.consultarSaga = consultarSaga;
    }

    @Operation(summary = "Consultar a Saga de uma ordem de serviço",
            description = "Estado atual, passo, prazo em vigor, compensações pendentes e a história completa "
                    + "(comandos enviados, eventos recebidos ou ignorados, prazos vencidos) da Saga da OS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saga encontrada",
                    content = @Content(schema = @Schema(implementation = SagaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço sem Saga", content = @Content)
    })
    @GetMapping("/{ordemServicoId}")
    public ResponseEntity<SagaResponse> consultar(@PathVariable UUID ordemServicoId) {
        return ResponseEntity.ok(SagaResponse.from(consultarSaga.consultar(ordemServicoId)));
    }
}
