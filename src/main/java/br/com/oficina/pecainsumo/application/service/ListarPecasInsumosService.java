package br.com.oficina.pecainsumo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.application.usecase.ListarPecasInsumosUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ListarPecasInsumosService implements ListarPecasInsumosUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public ListarPecasInsumosService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public List<PecaInsumo> listarPecasInsumos(ListarPecasInsumosQuery query) {
        return pecaInsumoRepository.buscarTodos().stream()
                .filter(peca -> query.marca() == null || peca.getMarca().equals(query.marca()))
                .filter(peca -> query.categoria() == null || peca.getCategoria().equals(query.categoria()))
                .filter(peca -> query.possuiReserva() == null ||
                        (query.possuiReserva() && peca.getQuantidadeReservada() > 0) ||
                        (!query.possuiReserva() && peca.getQuantidadeReservada() == 0))
                .toList();
    }
}
