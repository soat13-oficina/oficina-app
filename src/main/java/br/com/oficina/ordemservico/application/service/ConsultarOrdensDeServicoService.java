package br.com.oficina.ordemservico.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.application.usecase.ConsultarOrdensDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConsultarOrdensDeServicoService implements ConsultarOrdensDeServicoUseCase {
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConsultarOrdensDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public List<OrdemDeServico> consultarOrdensDeServico(ConsultarOrdensDeServicoQuery query) {
        return ordemDeServicoRepository.buscarTodas().stream()
                .filter(ordem -> query.numeroOrdemServico() == null
                        || ordem.getNumeroOrdemServico().equalsIgnoreCase(query.numeroOrdemServico()))
                .filter(ordem -> query.nomeCliente() == null
                        || ordem.getCliente().getNome().equalsIgnoreCase(query.nomeCliente()))
                .filter(ordem -> query.placaVeiculo() == null
                        || ordem.getVeiculo().getPlaca().equalsIgnoreCase(query.placaVeiculo()))
                .filter(ordem -> query.documentoCliente() == null
                        || (ordem.getCliente().getCpfOuCnpj() != null
                                && ordem.getCliente().getCpfOuCnpj().equals(query.documentoCliente())))
                .toList();
    }
}
