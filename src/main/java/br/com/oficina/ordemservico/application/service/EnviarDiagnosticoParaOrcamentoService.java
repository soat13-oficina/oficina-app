package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class EnviarDiagnosticoParaOrcamentoService implements EnviarDiagnosticoParaOrcamentoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase;

    public EnviarDiagnosticoParaOrcamentoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.cadastrarNovoOrcamentoUseCase = cadastrarNovoOrcamentoUseCase;
    }

    @Override
    public void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(request.numeroOrdemServico())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));

        ordemDeServico.enviarParaOrcamento();

        String numeroOrcamento = "ORC-" + UUID.randomUUID();
        cadastrarNovoOrcamentoUseCase.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                numeroOrcamento,
                ordemDeServico.getCliente().getId().toString(),
                ordemDeServico.getId().toString(),
                ordemDeServico.getFuncionario().getId().toString(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getVeiculo().getMarca(),
                ordemDeServico.getVeiculo().getModelo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                request.validade(),
                request.observacoes()));

        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
