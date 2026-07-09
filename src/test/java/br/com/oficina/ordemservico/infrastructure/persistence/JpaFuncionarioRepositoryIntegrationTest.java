package br.com.oficina.ordemservico.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.domain.model.Funcionario;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class JpaFuncionarioRepositoryIntegrationTest {

    @Autowired
    private JpaFuncionarioRepository repository;

    @Test
    void devePersistirEBuscarFuncionarioPorId() {
        Funcionario salvo = repository.salvar(new Funcionario("Ana Paula", "12345678909"));

        Funcionario encontrado = repository.buscarPorId(salvo.getId()).orElseThrow();

        assertEquals("Ana Paula", encontrado.getNome());
        assertEquals("12345678909", encontrado.getCpf());
        assertTrue(repository.buscarPorId(salvo.getId()).isPresent());
    }
}
