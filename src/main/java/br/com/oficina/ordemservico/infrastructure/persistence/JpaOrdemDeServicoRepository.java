package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaOrdemDeServicoRepository implements OrdemDeServicoRepository {
    private final SpringDataOrdemDeServicoRepository repository;

    public JpaOrdemDeServicoRepository(SpringDataOrdemDeServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(OrdemDeServico ordemDeServico) {
        repository.save(ordemDeServico);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorId(UUID ordemDeServicoId) {
        return repository.findById(ordemDeServicoId);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico) {
        return repository.findByNumeroOrdemServico(numeroOrdemServico);
    }

    @Override
    public List<OrdemDeServico> buscarPorFiltros(
            String numeroOrdemServico,
            String nomeCliente,
            String placaVeiculo,
            String documentoCliente) {
        Specification<OrdemDeServico> specification = Specification.where(campoIgual("numeroOrdemServico", numeroOrdemServico))
                .and(campoIgual("clienteNome", nomeCliente))
                .and(campoIgual("veiculoPlaca", placaVeiculo))
                .and(campoIgual("clienteDocumento", documentoCliente));
        return repository.findAll(specification);
    }

    @Override
    public List<OrdemDeServico> buscarAtivasPriorizadasPorFiltros(
            String numeroOrdemServico,
            String nomeCliente,
            String placaVeiculo,
            String documentoCliente) {
        return repository.buscarAtivasPriorizadasPorFiltros(
                numeroOrdemServico,
                nomeCliente,
                placaVeiculo,
                documentoCliente);
    }

    @Override
    public List<OrdemDeServico> buscarTodas() {
        return repository.findAll();
    }

    @Override
    public List<OrdemDeServico> buscarOrdensComExecucaoFinalizada() {
        return repository.findByIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();
    }

    @Override
    @Transactional
    public void excluirPorNumero(String numeroOrdemServico) {
        repository.deleteByNumeroOrdemServico(numeroOrdemServico);
    }

    @Override
    public boolean existePorFuncionarioId(UUID funcionarioId) {
        return repository.existsByFuncionarioId(funcionarioId);
    }

    private static <T> Specification<OrdemDeServico> campoIgual(String campo, T valor) {
        return (root, query, criteriaBuilder) -> valor == null ? null : criteriaBuilder.equal(root.get(campo), valor);
    }
}
