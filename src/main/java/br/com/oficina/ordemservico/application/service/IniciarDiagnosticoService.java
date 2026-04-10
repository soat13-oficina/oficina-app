package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class IniciarDiagnosticoService implements IniciarDiagnosticoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public IniciarDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void iniciarDiagnostico(IniciarDiagnosticoRequest request) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorId(request.ordemDeServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
