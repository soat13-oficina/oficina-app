package br.com.oficina.ordemservico.infrastructure.web.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EnviarDiagnosticoParaOrcamentoRequest(
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<String> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal valorPecas,
        LocalDateTime validade,
        String observacoes) {
}
