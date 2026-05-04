package br.com.oficina.veiculo.infrastructure.web;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.AlterarVeiculoUseCase;
import br.com.oficina.veiculo.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.application.usecase.ConsultarVeiculosUseCase;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.web.request.AlterarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.request.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.infrastructure.web.response.VeiculoResponse;

@RestController
@RequestMapping("/veiculos")
public class VeiculoController implements VeiculoControllerSwagger {
    private static final Logger log = LoggerFactory.getLogger(VeiculoController.class);

    private final AlterarVeiculoUseCase alterarVeiculoUseCase;
    private final CadastrarVeiculoUseCase cadastrarVeiculoUseCase;
    private final ConsultarVeiculosUseCase consultarVeiculosUseCase;
    private final ExcluirVeiculoUseCase excluirVeiculoUseCase;

    public VeiculoController(
            AlterarVeiculoUseCase alterarVeiculoUseCase,
            CadastrarVeiculoUseCase cadastrarVeiculoUseCase,
            ExcluirVeiculoUseCase excluirVeiculoUseCase,
            ConsultarVeiculosUseCase consultarVeiculosUseCase) {
        this.alterarVeiculoUseCase = alterarVeiculoUseCase;
        this.cadastrarVeiculoUseCase = cadastrarVeiculoUseCase;
        this.excluirVeiculoUseCase = excluirVeiculoUseCase;
        this.consultarVeiculosUseCase = consultarVeiculosUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody CadastrarVeiculoRequest request) {
        log.info("Recebida requisicao de cadastro de veiculo. placaInformada={}, marca={}, fabricante={}",
                request.placa(),
                request.marca(),
                request.fabricante());
        cadastrarVeiculoUseCase.cadastrarVeiculo(new CadastrarVeiculoCommand(
                request.placa(),
                request.marca(),
                request.modelo(),
                request.fabricante(),
                request.ano(),
                request.potencia(),
                request.cambio(),
                request.tipo(),
                request.clienteId()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{placa}")
                .buildAndExpand(Veiculo.normalizarPlaca(request.placa()))
                .toUri();
        log.info("Requisicao de cadastro de veiculo concluida. location={}", location);
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{placa}")
    public ResponseEntity<Void> alterar(@PathVariable String placa, @RequestBody AlterarVeiculoRequest request) {
        log.info("Recebida requisicao de alteracao de veiculo. placaInformada={}, marca={}, fabricante={}",
                placa,
                request.marca(),
                request.fabricante());
        alterarVeiculoUseCase.alterarVeiculo(new AlterarVeiculoCommand(
                placa,
                request.marca(),
                request.modelo(),
                request.fabricante(),
                request.ano(),
                request.potencia(),
                request.cambio(),
                request.tipo()));
        log.info("Requisicao de alteracao de veiculo concluida. placaInformada={}", placa);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<VeiculoResponse>> consultar(
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) Integer potencia,
            @RequestParam(required = false) String cambio,
            @RequestParam(required = false) TipoCombustivel tipo) {
        log.info("Recebida requisicao de consulta de veiculos. placaInformada={}, marca={}, fabricante={}, ano={}, tipo={}",
                placa,
                marca,
                fabricante,
                ano,
                tipo);
        List<VeiculoResponse> veiculos = consultarVeiculosUseCase
                .consultarVeiculos(new ConsultarVeiculosQuery(placa, ano, marca, fabricante, potencia, cambio, tipo))
                .stream()
                .map(VeiculoResponse::from)
                .toList();
        log.info("Requisicao de consulta de veiculos concluida. quantidadeVeiculos={}", veiculos.size());
        return ResponseEntity.ok(veiculos);
    }

    @DeleteMapping("/{placa}")
    public ResponseEntity<Void> excluir(@PathVariable String placa) {
        log.info("Recebida requisicao de exclusao de veiculo. placaInformada={}", placa);
        excluirVeiculoUseCase.excluirVeiculo(new ExcluirVeiculoCommand(placa));
        log.info("Requisicao de exclusao de veiculo concluida. placaInformada={}", placa);
        return ResponseEntity.noContent().build();
    }
}
