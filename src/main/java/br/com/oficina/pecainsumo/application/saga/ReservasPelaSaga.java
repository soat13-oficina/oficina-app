package br.com.oficina.pecainsumo.application.saga;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Passo {@code ReservarPecas} e compensacao {@code LiberarReserva} da Saga (ADR 0004, passo 4).
 *
 * <p>A reserva e tudo ou nada: com qualquer peca faltando, nada e reservado e a resposta e
 * {@code ReservaFalhou} com as faltantes - mesma regra que AprovarOrcamentoService aplicava de forma
 * sincrona. As pecas sao lidas com trava, em ordem de id, para duas OSs nao reservarem o mesmo
 * disponivel nem se travarem mutuamente.
 *
 * <p>Idempotente pelo estado: um comando reenviado por timeout (messageId novo) encontra as reservas
 * da OS e so repete a resposta.
 */
@Service
public class ReservasPelaSaga {
    private static final Logger log = LoggerFactory.getLogger(ReservasPelaSaga.class);

    private final PecaInsumoRepository pecas;
    private final ReservaPecaRepository reservas;
    private final EventosEstoque eventos;
    private final JsonMapper jsonMapper;

    public ReservasPelaSaga(PecaInsumoRepository pecas, ReservaPecaRepository reservas, EventosEstoque eventos, JsonMapper jsonMapper) {
        this.pecas = pecas;
        this.reservas = reservas;
        this.eventos = eventos;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservar(Envelope comando) {
        MensagensEstoque.ReservarPecas pedido = jsonMapper.treeToValue(comando.payload(), MensagensEstoque.ReservarPecas.class);
        UUID ordemId = comando.ordemServicoId();
        List<ReservaPeca> ativas = ativas(ordemId);
        if (!ativas.isEmpty()) {
            log.info("Pecas ja reservadas para a OS; resposta repetida. ordemServicoId={}", ordemId);
            eventos.responder(comando, MensagensEstoque.PECAS_RESERVADAS,
                    new MensagensEstoque.PecasReservadas(pedido.orcamentoId(), itens(ativas)));
            return;
        }

        Map<String, Integer> solicitadas = new TreeMap<>();
        pedido.itens().forEach(item -> solicitadas.merge(item.pecaInsumoId(), item.quantidade(), Integer::sum));

        List<MensagensEstoque.PecaFaltante> faltantes = new ArrayList<>();
        Map<String, PecaInsumo> encontradas = new TreeMap<>();
        solicitadas.forEach((pecaId, quantidade) -> {
            Optional<PecaInsumo> peca = pecas.buscarParaAtualizar(pecaId);
            int disponivel = peca.map(PecaInsumo::getQuantidadeDisponivel).orElse(0);
            if (peca.isEmpty() || disponivel < quantidade) {
                faltantes.add(new MensagensEstoque.PecaFaltante(pecaId, quantidade, Math.max(0, disponivel)));
            } else {
                encontradas.put(pecaId, peca.get());
            }
        });
        if (!faltantes.isEmpty()) {
            log.info("Reserva recusada por estoque insuficiente. ordemServicoId={}, faltantes={}", ordemId, faltantes);
            eventos.responder(comando, MensagensEstoque.RESERVA_FALHOU,
                    new MensagensEstoque.ReservaFalhou(pedido.orcamentoId(), faltantes));
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        solicitadas.forEach((pecaId, quantidade) -> {
            pecas.salvar(encontradas.get(pecaId).reservar(quantidade));
            reservas.salvar(ReservaPeca.nova(ordemId, pecaId, quantidade, agora));
        });
        log.info("Pecas reservadas para a OS. ordemServicoId={}, itens={}", ordemId, solicitadas);
        eventos.responder(comando, MensagensEstoque.PECAS_RESERVADAS,
                new MensagensEstoque.PecasReservadas(pedido.orcamentoId(), itens(ativas(ordemId))));
    }

    /** Compensacao: devolve ao disponivel o que esta reservado para a OS. Sem reserva, confirma com lista vazia. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void liberar(Envelope comando) {
        MensagensEstoque.LiberarReserva pedido = jsonMapper.treeToValue(comando.payload(), MensagensEstoque.LiberarReserva.class);
        List<ReservaPeca> ativas = ativas(comando.ordemServicoId());
        LocalDateTime agora = LocalDateTime.now();
        for (ReservaPeca reserva : ativas) {
            pecas.buscarParaAtualizar(reserva.pecaInsumoId())
                    .ifPresent(peca -> pecas.salvar(peca.liberarReserva(reserva.quantidade())));
            reservas.salvar(reserva.com(ReservaPeca.Status.LIBERADA, agora));
        }
        log.info("Reserva liberada pela Saga. ordemServicoId={}, itens={}, motivo={}",
                comando.ordemServicoId(), ativas.size(), pedido.motivo());
        eventos.responder(comando, MensagensEstoque.RESERVA_LIBERADA, new MensagensEstoque.ReservaLiberada(itens(ativas)));
    }

    private List<ReservaPeca> ativas(UUID ordemId) {
        return reservas.buscarPorOrdemDeServico(ordemId).stream().filter(ReservaPeca::ativa).toList();
    }

    private static List<MensagensEstoque.ItemPeca> itens(List<ReservaPeca> reservas) {
        return reservas.stream().map(r -> new MensagensEstoque.ItemPeca(r.pecaInsumoId(), r.quantidade())).toList();
    }
}
