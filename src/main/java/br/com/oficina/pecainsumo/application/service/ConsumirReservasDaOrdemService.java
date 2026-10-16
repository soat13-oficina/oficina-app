package br.com.oficina.pecainsumo.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.pecainsumo.application.usecase.ConsumirReservasDaOrdemUseCase;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;

/**
 * Conclusao do servico: as pecas reservadas para a OS saem do estoque. Antes da Fase 4 a conclusao
 * nao baixava nada (so a finalizacao "legada", inalcancavel, consumia) e a reserva ficava presa.
 */
@Service
public class ConsumirReservasDaOrdemService implements ConsumirReservasDaOrdemUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsumirReservasDaOrdemService.class);

    private final PecaInsumoRepository pecas;
    private final ReservaPecaRepository reservas;

    public ConsumirReservasDaOrdemService(PecaInsumoRepository pecas, ReservaPecaRepository reservas) {
        this.pecas = pecas;
        this.reservas = reservas;
    }

    @Override
    @Transactional
    public int consumirReservas(UUID ordemDeServicoId) {
        List<ReservaPeca> ativas = reservas.buscarPorOrdemDeServico(ordemDeServicoId).stream().filter(ReservaPeca::ativa).toList();
        LocalDateTime agora = LocalDateTime.now();
        for (ReservaPeca reserva : ativas) {
            pecas.buscarParaAtualizar(reserva.pecaInsumoId())
                    .ifPresent(peca -> pecas.salvar(peca.consumirReservada(reserva.quantidade())));
            reservas.salvar(reserva.com(ReservaPeca.Status.CONSUMIDA, agora));
        }
        log.info("Pecas reservadas consumidas na conclusao do servico. ordemServicoId={}, itens={}", ordemDeServicoId, ativas.size());
        return ativas.size();
    }
}
