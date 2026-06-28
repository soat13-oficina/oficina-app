package br.com.oficina.cliente.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataClienteRepository extends JpaRepository<Cliente, UUID> {
    /**
     * Busca por documento ignorando a formatação (pontos, traços, barras e espaços) armazenada na coluna,
     * comparando apenas os dígitos. A normalização é feita no banco (não em memória), preservando a correção
     * do CRI-002. O parâmetro DEVE chegar contendo apenas dígitos.
     */
    @Query("select c from Cliente c where "
            + "replace(replace(replace(replace(c.cpfOuCnpj, '.', ''), '-', ''), '/', ''), ' ', '') = :documento")
    Optional<Cliente> findByDocumentoNormalizado(@Param("documento") String documento);
}
