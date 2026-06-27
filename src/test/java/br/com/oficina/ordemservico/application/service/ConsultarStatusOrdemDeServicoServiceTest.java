package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.query.ConsultarStatusOrdemDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ConsultarStatusOrdemDeServicoServiceTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ConsultarStatusOrdemDeServicoService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void deveConsultarStatusERegistrarLogsDeEntradaEConclusao() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        repository.salvar(novaOrdem("OS-STATUS-01"));
        ConsultarStatusOrdemDeServicoService service = new ConsultarStatusOrdemDeServicoService(repository);

        OrdemDeServico resultado = service.consultarStatus(new ConsultarStatusOrdemDeServicoQuery("OS-STATUS-01"));

        assertEquals("OS-STATUS-01", resultado.getNumeroOrdemServico());

        List<ILoggingEvent> eventos = appender.list;
        assertEquals(2, eventos.size());
        assertEquals(Level.INFO, eventos.get(0).getLevel());
        assertTrue(eventos.get(0).getFormattedMessage().toLowerCase().contains("consultando"));
        assertTrue(eventos.get(0).getFormattedMessage().contains("OS-STATUS-01"));
        assertEquals(Level.INFO, eventos.get(1).getLevel());
        assertTrue(eventos.get(1).getFormattedMessage().toLowerCase().contains("concluida"));
        assertTrue(eventos.get(1).getFormattedMessage().contains("OS-STATUS-01"));
    }

    @Test
    void deveLancarRecursoNaoEncontradoParaNumeroInexistente() {
        TestOrdemDeServicoRepository repository = new TestOrdemDeServicoRepository();
        ConsultarStatusOrdemDeServicoService service = new ConsultarStatusOrdemDeServicoService(repository);

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.consultarStatus(new ConsultarStatusOrdemDeServicoQuery("OS-INEXISTENTE")));

        assertEquals("Ordem de servico nao encontrada para o numero informado.", exception.getMessage());
    }

    private OrdemDeServico novaOrdem(String numero) {
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        UUID veiculoId = UUID.fromString("51111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                null,
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", "12345678901"),
                Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF),
                Veiculo.reconstituir(
                        veiculoId, clienteId, "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
    }
}
