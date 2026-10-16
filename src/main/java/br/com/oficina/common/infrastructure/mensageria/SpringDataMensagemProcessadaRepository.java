package br.com.oficina.common.infrastructure.mensageria;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMensagemProcessadaRepository extends JpaRepository<MensagemProcessadaEntity, UUID> {
}
