package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class RegularizacaoOrdensAguardandoAprovacaoIntegrationTest {

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EnviarDiagnosticoParaOrcamentoUseCase enviarDiagnosticoParaOrcamentoUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deveReverterOrdensSemOrcamentoEPreservarAsComOrcamento() throws Exception {
        UUID clienteId = clienteRepository.salvar(new Cliente("Maria", "11111111111", TipoCliente.PF)).getId();

        // OS válida: passa pelo fluxo único, ficando em AGUARDANDO_APROVACAO COM orçamento associado.
        UUID idValida = persistirOrdemEmDiagnosticoConcluido("OS-VALIDA", clienteId);
        enviarDiagnosticoParaOrcamentoUseCase.enviarDiagnosticoParaOrcamento(new EnviarDiagnosticoParaOrcamentoRequest(
                "OS-VALIDA",
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 1, 0, 0),
                null));

        // OS órfã: simula o estado legado — AGUARDANDO_APROVACAO SEM orçamento.
        UUID idOrfa = persistirOrdemEmDiagnosticoConcluido("OS-ORFA", clienteId);
        entityManager.flush();
        jdbcTemplate.update("UPDATE ordens_de_servico SET status = 'AGUARDANDO_APROVACAO' WHERE id = ?", idOrfa);

        assertEquals("AGUARDANDO_APROVACAO", status(idValida));
        assertEquals("AGUARDANDO_APROVACAO", status(idOrfa));

        String sql = carregarMigracaoV17();
        int afetadas = jdbcTemplate.update(sql);

        assertEquals(1, afetadas, "apenas a OS órfã deve ser regularizada");
        assertEquals("DIAGNOSTICO_CONCLUIDO", status(idOrfa), "OS órfã revertida");
        assertEquals("AGUARDANDO_APROVACAO", status(idValida), "OS com orçamento preservada");

        // Idempotência: reexecutar não afeta mais nenhuma OS.
        int afetadasNaReexecucao = jdbcTemplate.update(sql);
        assertEquals(0, afetadasNaReexecucao, "migração é idempotente");
    }

    private UUID persistirOrdemEmDiagnosticoConcluido(String numero, UUID clienteId) {
        OrdemDeServico ordem = OrdemDeServico.abrir(
                null,
                numero,
                Funcionario.reconstituir(UUID.fromString("71111111-1111-1111-1111-111111111111"), "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes(StandardCharsets.UTF_8)),
                        clienteId,
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
        ordem.iniciarDiagnostico();
        ordem.concluirDiagnostico("Servico de diagnostico", List.of());
        ordemDeServicoRepository.salvar(ordem);
        return ordemDeServicoRepository.buscarPorNumero(numero).orElseThrow().getId();
    }

    private String status(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM ordens_de_servico WHERE id = ?", String.class, id);
    }

    private String carregarMigracaoV17() throws Exception {
        byte[] bytes = new ClassPathResource(
                "db/migration/V17__regularizar_ordens_aguardando_aprovacao_sem_orcamento.sql")
                .getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8).lines()
                .filter(linha -> !linha.trim().startsWith("--"))
                .reduce("", (acc, linha) -> acc + linha + "\n")
                .trim();
    }
}
