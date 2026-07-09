package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class AberturaComServicosEPecasTest {

    private static final UUID CLIENTE_ID = UUID.fromString("31111111-1111-1111-1111-111111111111");
    private static final UUID VEICULO_ID = UUID.fromString("51111111-1111-1111-1111-111111111111");
    private static final UUID FUNCIONARIO_ID = UUID.fromString("41111111-1111-1111-1111-111111111111");

    @Test
    void deveAbrirOrdemArmazenandoServicosEPecas() {
        OrdemDeServico ordem = OrdemDeServico.abrir(
                null,
                "OS-TESTE01",
                Funcionario.reconstituir(FUNCIONARIO_ID, "Joao", "12345678909"),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                veiculo(),
                List.of(new ServicoOrdem("Troca de pastilhas", new BigDecimal("150.00"))),
                List.of(new PecaPrevistaOrdem("PECA-1", 2)));

        assertEquals(StatusOrdemDeServico.OS_ABERTA, ordem.getStatus());
        assertEquals(1, ordem.getServicos().size());
        assertEquals("Troca de pastilhas", ordem.getServicos().get(0).getDescricao());
        assertEquals(1, ordem.getPecasPrevistas().size());
        assertEquals(2, ordem.getPecasPrevistas().get(0).getQuantidade());
    }

    @Test
    void deveAbrirOrdemSemServicosNemPecasComListasVazias() {
        OrdemDeServico ordem = OrdemDeServico.abrir(
                null,
                "OS-TESTE02",
                Funcionario.reconstituir(FUNCIONARIO_ID, "Joao", "12345678909"),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                veiculo(),
                null,
                null);

        assertTrue(ordem.getServicos().isEmpty());
        assertTrue(ordem.getPecasPrevistas().isEmpty());
    }

    @Test
    void deveFalharAoCriarServicoSemDescricao() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new ServicoOrdem(" ", new BigDecimal("10.00")));
        assertEquals("Descricao do servico e obrigatoria.", exception.getMessage());
    }

    @Test
    void deveFalharAoCriarServicoComValorNegativo() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new ServicoOrdem("Servico", new BigDecimal("-1.00")));
        assertEquals("Valor da mao de obra nao pode ser negativo.", exception.getMessage());
    }

    @Test
    void deveFalharAoCriarPecaPrevistaSemIdentificador() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new PecaPrevistaOrdem(null, 1));
        assertEquals("Identificador da peca prevista e obrigatorio.", exception.getMessage());
    }

    @Test
    void deveFalharAoCriarPecaPrevistaComQuantidadeInvalida() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new PecaPrevistaOrdem("PECA-1", 0));
        assertEquals("A quantidade da peca prevista deve ser maior que zero.", exception.getMessage());
    }

    private static Veiculo veiculo() {
        return Veiculo.reconstituir(
                VEICULO_ID,
                CLIENTE_ID,
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
    }
}
