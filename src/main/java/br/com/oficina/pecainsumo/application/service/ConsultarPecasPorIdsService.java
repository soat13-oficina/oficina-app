package br.com.oficina.pecainsumo.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.usecase.ConsultarPecasPorIdsUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ConsultarPecasPorIdsService implements ConsultarPecasPorIdsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarPecasPorIdsService.class);

    private final PecaInsumoRepository pecas;

    public ConsultarPecasPorIdsService(PecaInsumoRepository pecas) {
        this.pecas = pecas;
    }

    @Override
    public Resultado consultar(Collection<String> pecaInsumoIds) {
        if (pecaInsumoIds == null || pecaInsumoIds.isEmpty()) {
            throw new RegraDeNegocioException("Informe ao menos um id de peca/insumo.");
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>(pecaInsumoIds);
        if (ids.size() > MAXIMO_IDS) {
            throw new RegraDeNegocioException("No maximo " + MAXIMO_IDS + " pecas por consulta.");
        }
        List<PecaInsumo> encontradas = new ArrayList<>();
        List<String> inexistentes = new ArrayList<>();
        for (String id : ids) {
            Optional<PecaInsumo> peca = id == null || id.isBlank() ? Optional.empty() : pecas.buscarPorId(id);
            peca.ifPresentOrElse(encontradas::add, () -> inexistentes.add(id));
        }
        log.info("Consulta interna de pecas. solicitadas={}, encontradas={}, inexistentes={}",
                ids.size(), encontradas.size(), inexistentes.size());
        return new Resultado(encontradas, inexistentes);
    }
}
