package br.com.oficina.application.diagnostico;

import org.springframework.stereotype.Service;

import br.com.oficina.application.diagnostico.IniciarDiagnosticoUseCase;
import br.com.oficina.domain.model.ordemservico.OrdemDeServico;
import br.com.oficina.domain.repository.OrdemDeServicoRepository;

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
