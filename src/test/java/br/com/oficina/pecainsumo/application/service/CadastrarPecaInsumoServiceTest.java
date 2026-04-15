package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.application.command.CadastrarPecaInsumoCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class CadastrarPecaInsumoServiceTest {

    @Test
    void deveCadastrarPecaInsumo() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        CadastrarPecaInsumoService service = new CadastrarPecaInsumoService(repository);

        service.cadastrarPecaInsumo(new CadastrarPecaInsumoCommand(
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                "OB0986B01044",
                CategoriaPeca.FILTROS));

        PecaInsumo pecaSalva = repository.buscarTodos().get(0);
        assertNotNull(pecaSalva.getId());
        assertEquals("Filtro de óleo", pecaSalva.getDescricao());
        assertEquals("Bosch", pecaSalva.getMarca());
        assertEquals(new BigDecimal("45.90"), pecaSalva.getPreco());
        assertEquals(10, pecaSalva.getQuantidadeEstoque());
        assertEquals(0, pecaSalva.getQuantidadeReservada());
        assertEquals("OB0986B01044", pecaSalva.getCodigoReferencia());
        assertEquals(CategoriaPeca.FILTROS, pecaSalva.getCategoria());
    }

    @Test
    void deveCadastrarMultiplasPecasInsumos() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        CadastrarPecaInsumoService service = new CadastrarPecaInsumoService(repository);

        service.cadastrarPecaInsumo(new CadastrarPecaInsumoCommand(
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                "OB0986B01044",
                CategoriaPeca.FILTROS));

        service.cadastrarPecaInsumo(new CadastrarPecaInsumoCommand(
                "Pastilha de freio",
                "TRW",
                new BigDecimal("120.00"),
                20,
                "TRW001",
                CategoriaPeca.FREIOS));

        assertEquals(2, repository.buscarTodos().size());
    }
}
