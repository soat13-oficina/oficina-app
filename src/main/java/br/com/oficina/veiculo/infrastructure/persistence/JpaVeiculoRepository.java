package br.com.oficina.veiculo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaVeiculoRepository implements VeiculoRepository {
    private final SpringDataVeiculoRepository repository;

    public JpaVeiculoRepository(SpringDataVeiculoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(Veiculo veiculo) {
        repository.save(veiculo);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return repository.findByPlaca(Veiculo.normalizarPlaca(placa));
    }

    @Override
    public List<Veiculo> buscarPorFiltros(
            String placa,
            Integer ano,
            String marca,
            String fabricante,
            Integer potencia,
            String cambio,
            TipoCombustivel tipo) {
        String placaNormalizada = placa == null ? null : Veiculo.normalizarPlaca(placa);
        Specification<Veiculo> specification = Specification.where(campoIgual("placa", placaNormalizada))
                .and(campoIgual("ano", ano))
                .and(campoIgual("marca", marca))
                .and(campoIgual("fabricante", fabricante))
                .and(campoIgual("potencia", potencia))
                .and(campoIgual("cambio", cambio))
                .and(campoIgual("tipo", tipo));
        return repository.findAll(specification);
    }

    @Override
    public List<Veiculo> buscarTodos() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void excluirPorPlaca(String placa) {
        repository.deleteByPlaca(Veiculo.normalizarPlaca(placa));
    }

    private static <T> Specification<Veiculo> campoIgual(String campo, T valor) {
        return (root, query, criteriaBuilder) -> valor == null ? null : criteriaBuilder.equal(root.get(campo), valor);
    }
}
