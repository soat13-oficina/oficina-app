package br.com.oficina.application.diagnostico;

import org.springframework.stereotype.Service;

import br.com.oficina.application.diagnostico.ConcluirDiagnosticoUseCase;
import br.com.oficina.domain.model.ordemservico.OrdemDeServico;
import br.com.oficina.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirDiagnosticoService implements ConcluirDiagnosticoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConcluirDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void concluirDiagnostico(ConcluirDiagnosticoRequest request) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorId(request.ordemDeServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));
        ordemDeServico.concluirDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
