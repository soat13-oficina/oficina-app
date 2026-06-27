package br.com.oficina.cliente.domain.repository;

import java.util.UUID;

/**
 * Porta de domínio para verificar se um cliente possui vínculos (veículos ou ordens de serviço) que
 * impedem sua exclusão. Implementada na infraestrutura, evitando dependência circular de domínio entre
 * os módulos `cliente`, `veiculo` e `ordemservico` (spec 008 / CRI-001).
 */
public interface VerificadorVinculosCliente {
    boolean possuiVinculos(UUID clienteId);
}
