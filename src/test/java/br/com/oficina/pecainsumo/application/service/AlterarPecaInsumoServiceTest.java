package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.application.command.AlterarPecaInsumoCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class AlterarPecaInsumoServiceTest {

    @Test
    void deveAlterarPecaInsumoExistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo pecaOriginal = new PecaInsumo(
                "peca-id-1",
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                2,
                "OB0986B01044",
                CategoriaPeca.FILTROS);
        repository.salvar(pecaOriginal);
        AlterarPecaInsumoService service = new AlterarPecaInsumoService(repository);

        service.alterarPecaInsumo(new AlterarPecaInsumoCommand(
                "peca-id-1",
                "Filtro de ar",
                "Mann",
                new BigDecimal("55.00"),
                15,
                3,
                "MANN001",
                CategoriaPeca.FILTROS));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-id-1").orElseThrow();
        assertEquals("Filtro de ar", pecaAtualizada.getDescricao());
        assertEquals("Mann", pecaAtualizada.getMarca());
        assertEquals(new BigDecimal("55.00"), pecaAtualizada.getPreco());
        assertEquals(15, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(3, pecaAtualizada.getQuantidadeReservada());
        assertEquals("MANN001", pecaAtualizada.getCodigoReferencia());
        assertEquals(CategoriaPeca.FILTROS, pecaAtualizada.getCategoria());
    }

    @Test
    void deveAlterarCategoriaDaPecaInsumo() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo pecaOriginal = new PecaInsumo(
                "peca-id-2",
                "Disco de freio",
                "TRW",
                new BigDecimal("180.00"),
                5,
                0,
                "TRW002",
                CategoriaPeca.FREIOS);
        repository.salvar(pecaOriginal);
        AlterarPecaInsumoService service = new AlterarPecaInsumoService(repository);

        service.alterarPecaInsumo(new AlterarPecaInsumoCommand(
                "peca-id-2",
                "Disco de freio ventilado",
                "TRW",
                new BigDecimal("220.00"),
                5,
                0,
                "TRW003",
                CategoriaPeca.FREIOS));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-id-2").orElseThrow();
        assertEquals("Disco de freio ventilado", pecaAtualizada.getDescricao());
        assertEquals(new BigDecimal("220.00"), pecaAtualizada.getPreco());
        assertEquals("TRW003", pecaAtualizada.getCodigoReferencia());
    }
}
