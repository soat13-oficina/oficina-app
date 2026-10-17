package br.com.oficina.ordemservico.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.port.CatalogoPecas;
import br.com.oficina.ordemservico.application.usecase.CriarNovaOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrdemDeServicoCriada;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.ServicoOrdem;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.application.PassosDaSaga;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class CriarNovaOrdemDeServicoService implements CriarNovaOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(CriarNovaOrdemDeServicoService.class);

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final CatalogoPecas catalogoPecas;
    private final ApplicationEventPublisher eventPublisher;
    private final PassosDaSaga passosDaSaga;

    public CriarNovaOrdemDeServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            FuncionarioRepository funcionarioRepository,
            OrdemDeServicoRepository ordemDeServicoRepository,
            CatalogoPecas catalogoPecas,
            ApplicationEventPublisher eventPublisher) {
        this(clienteRepository, veiculoRepository, funcionarioRepository, ordemDeServicoRepository, catalogoPecas, eventPublisher, PassosDaSaga.NENHUM);
    }

    @Autowired
    public CriarNovaOrdemDeServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            FuncionarioRepository funcionarioRepository,
            OrdemDeServicoRepository ordemDeServicoRepository,
            CatalogoPecas catalogoPecas,
            ApplicationEventPublisher eventPublisher,
            PassosDaSaga passosDaSaga) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.catalogoPecas = catalogoPecas;
        this.eventPublisher = eventPublisher;
        this.passosDaSaga = passosDaSaga;
    }

    /** A ordem e a sua Saga nascem na mesma transacao (ADR 0004). */
    @Override
    @Transactional
    public String criarNovaOrdemDeServico(CriarOrdemDeServicoCommand command) {
        log.info("Iniciando criacao de ordem de servico. clienteId={}, funcionarioId={}, placaVeiculo={}",
                command.clienteId(),
                command.funcionarioId(),
                command.placaVeiculo());
        Cliente cliente = clienteRepository.buscarPorId(paraUuid(command.clienteId(), "Identificador do cliente invalido."))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placaVeiculo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        Funcionario funcionario = funcionarioRepository
                .buscarPorId(paraUuid(command.funcionarioId(), "Identificador do funcionario invalido."))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RegraDeNegocioException("Veiculo informado nao pertence ao cliente selecionado.");
        }

        List<ServicoOrdem> servicos = command.servicos().stream()
                .map(item -> new ServicoOrdem(item.descricao(), item.valorMaoDeObra()))
                .toList();
        validarPecas(command.pecasPrevistas().stream().map(CriarOrdemDeServicoCommand.PecaItem::pecaInsumoId).toList());
        List<PecaPrevistaOrdem> pecasPrevistas = command.pecasPrevistas().stream()
                .map(item -> new PecaPrevistaOrdem(item.pecaInsumoId(), item.quantidade()))
                .toList();

        UUID identificador = UUID.randomUUID();
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-" + identificador.toString().substring(0, 8).toUpperCase(),
                funcionario,
                cliente,
                veiculo,
                servicos,
                pecasPrevistas);

        ordemDeServicoRepository.salvar(ordemDeServico);
        passosDaSaga.iniciar(ordemDeServico);
        eventPublisher.publishEvent(OrdemDeServicoCriada.de(ordemDeServico));
        log.info(
                "Ordem de servico criada com sucesso. numeroOrdemServico={}, clienteId={}, funcionarioId={}, placaVeiculo={}, servicos={}, pecas={}",
                ordemDeServico.getNumeroOrdemServico(),
                cliente.getId(),
                funcionario.getId(),
                veiculo.getPlaca(),
                servicos.size(),
                pecasPrevistas.size());
        return ordemDeServico.getNumeroOrdemServico();
    }

    private void validarPecas(List<String> pecaInsumoIds) {
        if (pecaInsumoIds.isEmpty()) {
            return;
        }
        Set<String> inexistentes = catalogoPecas.inexistentes(pecaInsumoIds);
        if (!inexistentes.isEmpty()) {
            throw new RecursoNaoEncontradoException(
                    "Peca nao encontrada para o identificador informado: " + String.join(", ", inexistentes));
        }
    }

    private UUID paraUuid(String valor, String mensagemErro) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException(mensagemErro);
        }
    }
}
