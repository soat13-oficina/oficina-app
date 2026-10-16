package br.com.oficina.common.infrastructure.mensageria.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.infrastructure.mensageria.PublicadorOutbox;
import br.com.oficina.common.infrastructure.mensageria.SpringDataMensagemProcessadaRepository;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import tools.jackson.databind.json.JsonMapper;

/**
 * Transporte SQS contra o LocalStack (ADR 0005): o outbox manda pelo SQS so as filas ligadas e o
 * receptor entrega ao mesmo consumidor idempotente do transporte em memoria.
 */
@Testcontainers
@SpringBootTest(properties = {
        "mensageria.sqs.habilitado=true",
        "mensageria.sqs.envio=billing-commands",
        "mensageria.sqs.consumo=os-saga-replies",
        "mensageria.sqs.ambiente=teste",
        "mensageria.sqs.espera=PT1S",
        "mensageria.sqs.receptor-automatico=false"
})
class TransporteSqsLocalStackTest {

    static {
        // Cadeia padrao de credenciais do SDK; o LocalStack aceita qualquer par.
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
            .withServices(LocalStackContainer.Service.SQS);

    private static SqsClient sqsDoTeste;
    private static String urlBillingCommands;
    private static String urlReplies;
    private static String urlRepliesDlq;

    @DynamicPropertySource
    static void localstack(DynamicPropertyRegistry registro) {
        registro.add("mensageria.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registro.add("mensageria.sqs.regiao", LOCALSTACK::getRegion);
    }

    @BeforeAll
    static void criarFilas() {
        sqsDoTeste = SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
        urlBillingCommands = criarFifo("oficina-teste-billing-commands.fifo", Map.of());
        urlRepliesDlq = criarFifo("oficina-teste-os-saga-replies-dlq.fifo", Map.of());
        String arnDlq = sqsDoTeste.getQueueAttributes(r -> r.queueUrl(urlRepliesDlq).attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);
        urlReplies = criarFifo("oficina-teste-os-saga-replies.fifo", Map.of(
                QueueAttributeName.VISIBILITY_TIMEOUT, "1",
                QueueAttributeName.REDRIVE_POLICY, "{\"deadLetterTargetArn\":\"" + arnDlq + "\",\"maxReceiveCount\":\"5\"}"));
    }

    private static String criarFifo(String nome, Map<QueueAttributeName, String> atributos) {
        Map<QueueAttributeName, String> todos = new java.util.HashMap<>(atributos);
        todos.put(QueueAttributeName.FIFO_QUEUE, "true");
        return sqsDoTeste.createQueue(r -> r.queueName(nome).attributes(todos)).queueUrl();
    }

    @Autowired
    private PublicadorMensagens publicador;

    @Autowired
    private PublicadorOutbox publicadorOutbox;

    @Autowired
    private ReceptorSqs receptor;

    @Autowired
    private FabricaEnvelopes fabrica;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private SpringDataOutboxMensagemRepository outbox;

    @Autowired
    private SpringDataMensagemProcessadaRepository processadas;

    @Autowired
    private ManipuladorDeTeste manipulador;

    @BeforeEach
    void limpar() {
        outbox.deleteAll();
        processadas.deleteAll();
        manipulador.reiniciar();
        for (String url : List.of(urlBillingCommands, urlReplies, urlRepliesDlq)) {
            sqsDoTeste.purgeQueue(r -> r.queueUrl(url));
        }
    }

    @Test
    void filaLigadaSaiPeloSqsComGrupoDeduplicacaoEAtributosEAsOutrasContinuamEmMemoria() {
        UUID ordem = UUID.randomUUID();
        MDC.put(FabricaEnvelopes.MDC_CORRELATION_ID, "req-123");
        Envelope paraOBilling;
        Envelope paraAExecution;
        try {
            paraOBilling = fabrica.novo("TesteSqs", UUID.randomUUID(), ordem, Map.of("campo", "valor"));
            paraAExecution = fabrica.novo("TesteSqs", UUID.randomUUID(), ordem, Map.of("campo", "local"));
        } finally {
            MDC.clear();
        }
        transacao.executeWithoutResult(s -> {
            publicador.publicar(Filas.BILLING_COMMANDS, paraOBilling);
            publicador.publicar(Filas.EXECUTION_COMMANDS, paraAExecution);
        });

        assertThat(publicadorOutbox.publicarPendentes()).isEqualTo(2);

        assertThat(manipulador.recebidas).as("execution-commands ainda e entregue no processo").containsExactly(paraAExecution);
        List<Message> naFila = receber(urlBillingCommands);
        assertThat(naFila).singleElement().satisfies(m -> {
            assertThat(jsonMapper.readValue(m.body(), Envelope.class)).isEqualTo(paraOBilling);
            assertThat(m.attributes()).containsEntry(MessageSystemAttributeName.MESSAGE_GROUP_ID, ordem.toString())
                    .containsEntry(MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, paraOBilling.messageId().toString());
            assertThat(m.messageAttributes().get("type").stringValue()).isEqualTo("TesteSqs");
            assertThat(m.messageAttributes().get("version").stringValue()).isEqualTo("1");
            assertThat(m.messageAttributes().get("correlationId").stringValue()).isEqualTo("req-123");
        });
    }

    @Test
    void receptorEntregaAoConsumidorEApagaDaFilaUmaVezSo() {
        Envelope evento = fabrica.novo("TesteSqs", UUID.randomUUID(), UUID.randomUUID(), Map.of("campo", "valor"));
        enviarParaReplies(evento, UUID.randomUUID().toString());
        enviarParaReplies(evento, UUID.randomUUID().toString()); // reentrega: mesmo messageId

        assertThat(receptor.receberLote(Filas.OS_SAGA_REPLIES)).isEqualTo(2);

        assertThat(manipulador.recebidas).containsExactly(evento);
        assertThat(processadas.existsById(evento.messageId())).isTrue();
        assertThat(receber(urlReplies)).isEmpty();
    }

    @Test
    void mensagemForaDoContratoVaiDiretoParaADlq() {
        sqsDoTeste.sendMessage(r -> r.queueUrl(urlReplies).messageGroupId("grupo-x")
                .messageDeduplicationId(UUID.randomUUID().toString()).messageBody("{nao e json"));
        Envelope semManipulador = fabrica.novo("TipoQueNinguemTrata", null, UUID.randomUUID(), Map.of());
        enviarParaReplies(semManipulador, UUID.randomUUID().toString());

        assertThat(receptor.receberLote(Filas.OS_SAGA_REPLIES)).isEqualTo(2);

        assertThat(receber(urlReplies)).isEmpty();
        List<Message> naDlq = receber(urlRepliesDlq);
        assertThat(naDlq).extracting(Message::body).hasSize(2).contains("{nao e json");
        assertThat(naDlq).allSatisfy(m -> assertThat(m.messageAttributes()).containsKey("erro"));
    }

    @Test
    void falhaSeguraAsSeguintesDaMesmaOsAteDarCertoMasNaoAsDasOutras() throws InterruptedException {
        manipulador.falhasRestantes.set(1);
        UUID ordemA = UUID.randomUUID();
        Envelope a1 = fabrica.novo("TesteFalhaControlada", null, ordemA, Map.of("n", 1));
        Envelope a2 = fabrica.novo("TesteSqs", null, ordemA, Map.of("n", 2));
        Envelope b1 = fabrica.novo("TesteSqs", null, UUID.randomUUID(), Map.of("n", 3));
        enviarParaReplies(a1, a1.messageId().toString());
        enviarParaReplies(a2, a2.messageId().toString());
        enviarParaReplies(b1, b1.messageId().toString());

        // O lote pode vir partido; processa ate esvaziar (a1 volta depois do visibility timeout de 1 s).
        for (int i = 0; i < 15 && manipulador.recebidas.size() < 3; i++) {
            receptor.receberLote(Filas.OS_SAGA_REPLIES);
        }

        assertThat(manipulador.falhas).hasValue(1);
        assertThat(manipulador.recebidas).containsExactlyInAnyOrder(a1, a2, b1);
        assertThat(manipulador.recebidas.indexOf(a1)).as("ordem da OS preservada").isLessThan(manipulador.recebidas.indexOf(a2));
        assertThat(receber(urlRepliesDlq)).isEmpty();
    }

    private void enviarParaReplies(Envelope envelope, String deduplicacao) {
        String corpo = jsonMapper.writeValueAsString(envelope);
        sqsDoTeste.sendMessage(r -> r.queueUrl(urlReplies).messageGroupId(envelope.ordemServicoId().toString())
                .messageDeduplicationId(deduplicacao).messageBody(corpo));
    }

    private static List<Message> receber(String url) {
        return sqsDoTeste.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)
                .messageSystemAttributeNames(MessageSystemAttributeName.ALL).messageAttributeNames("All")).messages();
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        ManipuladorDeTeste manipuladorDeTeste() {
            return new ManipuladorDeTeste();
        }
    }

    static class ManipuladorDeTeste implements ManipuladorMensagem {
        final List<Envelope> recebidas = java.util.Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger falhasRestantes = new AtomicInteger();
        final AtomicInteger falhas = new AtomicInteger();

        void reiniciar() {
            recebidas.clear();
            falhasRestantes.set(0);
            falhas.set(0);
        }

        @Override
        public Set<String> tipos() {
            return Set.of("TesteSqs", "TesteFalhaControlada");
        }

        @Override
        public void tratar(Envelope envelope) {
            if ("TesteFalhaControlada".equals(envelope.type()) && falhasRestantes.getAndDecrement() > 0) {
                falhas.incrementAndGet();
                throw new IllegalStateException("falha simulada");
            }
            recebidas.add(envelope);
        }
    }
}
