package br.com.oficina.ordemservico.saga.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;

/** O agendamento fica desligado nos testes de contexto; aqui so o contrato do disparo. */
class AgendadorPrazosSagaTest {

    @Test
    void cadaCicloProcessaOsPrazosVencidos() {
        AtomicInteger chamadas = new AtomicInteger();
        OrquestradorSagaOrdemServico orquestrador = new OrquestradorSagaOrdemServico(
                null, null, null, null, null, null, null, null, null, null, 3) {
            @Override
            public int processarPrazosVencidos() {
                return chamadas.incrementAndGet();
            }
        };

        new AgendadorPrazosSaga(orquestrador).verificar();

        assertThat(chamadas).hasValue(1);
    }

    @Test
    void falhaDoCicloInteiroNaoDerrubaOAgendamento() {
        OrquestradorSagaOrdemServico quebrado = new OrquestradorSagaOrdemServico(
                null, null, null, null, null, null, null, null, null, null, 3) {
            @Override
            public int processarPrazosVencidos() {
                throw new IllegalStateException("banco fora");
            }
        };

        assertThatCode(() -> new AgendadorPrazosSaga(quebrado).verificar()).doesNotThrowAnyException();
    }
}
