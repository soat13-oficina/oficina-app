package br.com.oficina.support.saga;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;
import tools.jackson.databind.JsonNode;

/**
 * Execution Service de mentira para os testes do OS: fila, diagnostico, execucao e estoque moram no
 * {@code oficina-execution-service} desde o Passo 4. Atende {@code execution-commands} no proprio processo
 * (fila nao ligada ao SQS nos testes) e responde como {@code docs/contratos} manda - o
 * {@code ContratoDasMensagensDaSagaTest} confere. Tambem faz o papel do mecanico: inicia e conclui o
 * diagnostico, inicia, conclui e aborta a execucao (eventos espontaneos, sem {@code causationId}).
 *
 * <p>Estado so em memoria e simplificado: o que importa aqui e a Saga do OS, nao as regras da Execution
 * (essas tem os testes do proprio servico). Em particular nao valida as pecas do diagnostico - assim um
 * teste consegue fazer o Billing receber uma peca que sumiu do catalogo.
 */
public class ExecutionSimulado implements ManipuladorMensagem {

    public static final String SERVICO = "oficina-execution-service";
    public static final Map<String, String> MECANICO = Map.of("id", "mecanico@oficina.com", "nome", "Beto Mecanico");

    public enum Etapa { AGUARDANDO_DIAGNOSTICO, EM_DIAGNOSTICO, DIAGNOSTICO_CONCLUIDO, AGUARDANDO_EXECUCAO, EM_EXECUCAO,
        CONCLUIDA, ABORTADA, CANCELADA }

    public static final class ExecucaoSimulada {
        private final UUID ordemServicoId;
        private final UUID sagaId;
        private volatile Etapa etapa = Etapa.AGUARDANDO_DIAGNOSTICO;
        private volatile List<Map<String, Object>> servicosPrevistos = List.of();
        private volatile List<Map<String, Object>> pecasPrevistas = List.of();
        private volatile List<Map<String, Object>> pecasDoDiagnostico = List.of();

        ExecucaoSimulada(UUID ordemServicoId, UUID sagaId) {
            this.ordemServicoId = ordemServicoId;
            this.sagaId = sagaId;
        }

        public Etapa getEtapa() {
            return etapa;
        }

        public List<Map<String, Object>> getServicosPrevistos() {
            return servicosPrevistos;
        }

        public List<Map<String, Object>> getPecasPrevistas() {
            return pecasPrevistas;
        }
    }

    private final PublicadorMensagens publicador;
    private final FabricaEnvelopes fabrica;
    private final TransactionTemplate transacao;
    private final CatalogoSimulado catalogo;
    private final Map<UUID, ExecucaoSimulada> execucoes = new ConcurrentHashMap<>();
    // Reservas ativas por OS: pecaInsumoId -> quantidade.
    private final Map<UUID, Map<String, Integer>> reservas = new ConcurrentHashMap<>();

    public ExecutionSimulado(PublicadorMensagens publicador, FabricaEnvelopes fabrica, TransactionTemplate transacao,
                             CatalogoSimulado catalogo) {
        this.publicador = publicador;
        this.fabrica = fabrica.paraServico(SERVICO);
        this.transacao = transacao;
        this.catalogo = catalogo;
    }

    public void limpar() {
        execucoes.clear();
        reservas.clear();
    }

    // ------------------------------------------------------------------ consultas para os testes

    public Optional<ExecucaoSimulada> execucaoDaOrdem(UUID ordemServicoId) {
        return Optional.ofNullable(execucoes.get(ordemServicoId));
    }

    public Etapa etapa(UUID ordemServicoId) {
        return execucoes.get(ordemServicoId).etapa;
    }

    // ------------------------------------------------------------------ comandos da Saga

    @Override
    public Set<String> tipos() {
        return Set.of(MensagensSaga.ENFILEIRAR_DIAGNOSTICO, MensagensSaga.REABRIR_DIAGNOSTICO, MensagensSaga.RESERVAR_PECAS,
                MensagensSaga.ENFILEIRAR_EXECUCAO, MensagensSaga.LIBERAR_RESERVA, MensagensSaga.CANCELAR_EXECUCAO);
    }

    @Override
    public void tratar(Envelope comando) {
        switch (comando.type()) {
            case MensagensSaga.ENFILEIRAR_DIAGNOSTICO -> {
                ExecucaoSimulada execucao = execucoes.computeIfAbsent(comando.ordemServicoId(), id -> {
                    ExecucaoSimulada nova = new ExecucaoSimulada(id, comando.sagaId());
                    nova.servicosPrevistos = lista(comando.payload().path("servicosPrevistos"));
                    nova.pecasPrevistas = lista(comando.payload().path("pecasPrevistas"));
                    return nova;
                });
                responder(comando, MensagensSaga.DIAGNOSTICO_ENFILEIRADO, Map.of("posicaoNaFila", posicao(execucao)));
            }
            case MensagensSaga.REABRIR_DIAGNOSTICO -> {
                ExecucaoSimulada execucao = execucao(comando.ordemServicoId());
                if (execucao.etapa == Etapa.DIAGNOSTICO_CONCLUIDO) {
                    execucao.etapa = Etapa.AGUARDANDO_DIAGNOSTICO;
                }
                responder(comando, MensagensSaga.DIAGNOSTICO_REABERTO, Map.of("reabertoEm", agora()));
            }
            case MensagensSaga.RESERVAR_PECAS -> reservar(comando);
            case MensagensSaga.ENFILEIRAR_EXECUCAO -> {
                ExecucaoSimulada execucao = execucao(comando.ordemServicoId());
                if (execucao.etapa == Etapa.DIAGNOSTICO_CONCLUIDO) {
                    execucao.etapa = Etapa.AGUARDANDO_EXECUCAO;
                }
                responder(comando, MensagensSaga.EXECUCAO_ENFILEIRADA, Map.of("posicaoNaFila", posicao(execucao)));
            }
            case MensagensSaga.LIBERAR_RESERVA -> {
                Map<String, Integer> ativas = reservas.remove(comando.ordemServicoId());
                List<Map<String, Object>> itens = new ArrayList<>();
                if (ativas != null) {
                    ativas.forEach((pecaId, quantidade) -> {
                        catalogo.liberar(pecaId, quantidade);
                        itens.add(Map.of("pecaInsumoId", pecaId, "quantidade", quantidade));
                    });
                }
                responder(comando, MensagensSaga.RESERVA_LIBERADA, Map.of("itens", itens));
            }
            case MensagensSaga.CANCELAR_EXECUCAO -> {
                ExecucaoSimulada execucao = execucoes.get(comando.ordemServicoId());
                if (execucao != null && execucao.etapa != Etapa.CONCLUIDA && execucao.etapa != Etapa.ABORTADA) {
                    execucao.etapa = Etapa.CANCELADA;
                }
                responder(comando, MensagensSaga.EXECUCAO_CANCELADA, Map.of("canceladaEm", agora()));
            }
            default -> throw new IllegalArgumentException(comando.type());
        }
    }

    /** Tudo ou nada: com qualquer peca faltando nada e reservado. Reenvio da mesma OS repete a resposta. */
    private void reservar(Envelope comando) {
        UUID ordemId = comando.ordemServicoId();
        String orcamentoId = comando.payload().get("orcamentoId").asString();
        Map<String, Integer> existentes = reservas.get(ordemId);
        if (existentes != null) {
            responder(comando, MensagensSaga.PECAS_RESERVADAS, Map.of("orcamentoId", orcamentoId, "itens", itens(existentes)));
            return;
        }
        Map<String, Integer> pedidas = new TreeMap<>();
        for (JsonNode item : comando.payload().path("itens")) {
            pedidas.merge(item.get("pecaInsumoId").asString(), item.get("quantidade").asInt(), Integer::sum);
        }
        List<Map<String, Object>> faltantes = new ArrayList<>();
        pedidas.forEach((pecaId, quantidade) -> {
            int disponivel = catalogo.buscar(pecaId).map(CatalogoSimulado.Peca::getDisponivel).orElse(0);
            if (disponivel < quantidade) {
                Map<String, Object> faltante = new LinkedHashMap<>();
                faltante.put("pecaInsumoId", pecaId);
                faltante.put("solicitada", quantidade);
                faltante.put("disponivel", Math.max(0, disponivel));
                faltantes.add(faltante);
            }
        });
        if (!faltantes.isEmpty()) {
            responder(comando, MensagensSaga.RESERVA_FALHOU, Map.of("orcamentoId", orcamentoId, "faltantes", faltantes));
            return;
        }
        pedidas.forEach(catalogo::reservar);
        if (!pedidas.isEmpty()) {
            reservas.put(ordemId, pedidas);
        }
        responder(comando, MensagensSaga.PECAS_RESERVADAS, Map.of("orcamentoId", orcamentoId, "itens", itens(pedidas)));
    }

    // ------------------------------------------------------------------ o mecanico

    public void iniciarDiagnostico(UUID ordemServicoId) {
        ExecucaoSimulada execucao = esperar(ordemServicoId, Etapa.AGUARDANDO_DIAGNOSTICO);
        execucao.etapa = Etapa.EM_DIAGNOSTICO;
        espontaneo(execucao, MensagensSaga.DIAGNOSTICO_INICIADO, Map.of("mecanico", MECANICO, "iniciadoEm", agora()));
    }

    /** Servicos como {@code descricao -> mao de obra} e pecas como {@code pecaInsumoId -> quantidade}. */
    public void concluirDiagnostico(UUID ordemServicoId, String descricao, Map<String, String> servicos, Map<String, Integer> pecas) {
        ExecucaoSimulada execucao = esperar(ordemServicoId, Etapa.EM_DIAGNOSTICO);
        execucao.etapa = Etapa.DIAGNOSTICO_CONCLUIDO;
        List<Map<String, Object>> servicosDoEvento = new ArrayList<>();
        servicos.forEach((servico, maoDeObra) -> servicosDoEvento.add(Map.of("descricao", servico, "valorMaoDeObra", maoDeObra)));
        List<Map<String, Object>> pecasDoEvento = new ArrayList<>();
        pecas.forEach((pecaId, quantidade) -> pecasDoEvento.add(Map.of("pecaInsumoId", pecaId, "quantidade", quantidade)));
        execucao.pecasDoDiagnostico = pecasDoEvento;
        espontaneo(execucao, MensagensSaga.DIAGNOSTICO_CONCLUIDO, Map.of(
                "mecanico", MECANICO, "descricaoDiagnostico", descricao, "servicos", servicosDoEvento,
                "pecas", pecasDoEvento, "concluidoEm", agora()));
    }

    public void iniciarExecucao(UUID ordemServicoId) {
        ExecucaoSimulada execucao = esperar(ordemServicoId, Etapa.AGUARDANDO_EXECUCAO);
        execucao.etapa = Etapa.EM_EXECUCAO;
        espontaneo(execucao, MensagensSaga.EXECUCAO_INICIADA, Map.of("mecanico", MECANICO, "iniciadaEm", agora()));
    }

    /** Conclui o servico e baixa do estoque o que a Saga reservou para a OS. */
    public void concluirExecucao(UUID ordemServicoId) {
        ExecucaoSimulada execucao = esperar(ordemServicoId, Etapa.EM_EXECUCAO);
        execucao.etapa = Etapa.CONCLUIDA;
        Map<String, Integer> consumidas = reservas.remove(ordemServicoId);
        if (consumidas != null) {
            consumidas.forEach(catalogo::consumir);
        }
        espontaneo(execucao, MensagensSaga.EXECUCAO_CONCLUIDA, Map.of(
                "mecanico", MECANICO, "pecasConsumidas", itens(consumidas == null ? Map.of() : consumidas), "concluidaEm", agora()));
    }

    /** Aborta; as reservas ficam para a compensacao {@code LiberarReserva} da Saga. */
    public void abortarExecucao(UUID ordemServicoId, String motivo) {
        ExecucaoSimulada execucao = esperar(ordemServicoId, Etapa.EM_EXECUCAO);
        execucao.etapa = Etapa.ABORTADA;
        espontaneo(execucao, MensagensSaga.EXECUCAO_FALHOU, Map.of("mecanico", MECANICO, "motivo", motivo, "abortadaEm", agora()));
    }

    // ------------------------------------------------------------------ apoio

    private ExecucaoSimulada execucao(UUID ordemServicoId) {
        ExecucaoSimulada execucao = execucoes.get(ordemServicoId);
        if (execucao == null) {
            throw new IllegalStateException("A OS nao esta na Execution: " + ordemServicoId);
        }
        return execucao;
    }

    private ExecucaoSimulada esperar(UUID ordemServicoId, Etapa esperada) {
        ExecucaoSimulada execucao = execucao(ordemServicoId);
        if (execucao.etapa != esperada) {
            throw new IllegalStateException("Execucao em " + execucao.etapa + "; esperado " + esperada);
        }
        return execucao;
    }

    private int posicao(ExecucaoSimulada execucao) {
        return (int) execucoes.values().stream().filter(e -> e.etapa == execucao.etapa).count();
    }

    private static List<Map<String, Object>> itens(Map<String, Integer> quantidades) {
        List<Map<String, Object>> itens = new ArrayList<>();
        quantidades.forEach((pecaId, quantidade) -> itens.add(Map.of("pecaInsumoId", pecaId, "quantidade", quantidade)));
        return itens;
    }

    private static List<Map<String, Object>> lista(JsonNode no) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (JsonNode item : no) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            item.properties().forEach(campo -> mapa.put(campo.getKey(), campo.getValue().isNumber()
                    ? (Object) campo.getValue().asInt() : campo.getValue().asString()));
            itens.add(mapa);
        }
        return itens;
    }

    private void responder(Envelope comando, String tipo, Object payload) {
        publicador.publicar(Filas.OS_SAGA_REPLIES, fabrica.resposta(comando, tipo, payload));
    }

    private void espontaneo(ExecucaoSimulada execucao, String tipo, Object payload) {
        transacao.executeWithoutResult(s -> publicador.publicar(Filas.OS_SAGA_REPLIES,
                fabrica.novo(tipo, execucao.sagaId, execucao.ordemServicoId, payload)));
    }

    private static String agora() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
