# Oficina API - Tech Challenge

Backend monolítico para gestão de Oficina Mecânica, desenvolvido com **Java 21 + Spring Boot 4 + PostgreSQL**.

---

## 🔐 Autenticação (JWT)

A aplicação protege as rotas administrativas com **JWT Bearer Token**. A autenticação é baseada na entidade `Usuario` persistida no PostgreSQL.

**Fluxo de uso:**
1. `POST /api/auth/register` - Cria um novo usuário (`{ "email": "...", "senha": "..." }`).
2. `POST /api/auth/login` - Autentica e retorna o token JWT.
3. Envie o header `Authorization: Bearer <TOKEN>` nas chamadas protegidas.

---

## 📘 Swagger / Documentação

Com a aplicação rodando, acesse:

👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

Para autenticar no Swagger:
1. Faça login pela rota `/api/auth/login` e copie o token.
2. Clique em **Authorize** (topo da página).
3. Cole o token no campo e confirme — todos os endpoints passam a enviar o JWT automaticamente.

---

## 🚀 Executando Localmente

### Pré-requisitos
- Java 21+
- Docker & Docker Compose

### Subindo o ambiente

```bash
# Sobe apenas o banco PostgreSQL
docker-compose up -d db

# Roda a aplicação (O Hibernate gera o schema automaticamente)
./mvnw spring-boot:run
```

### Rodando os testes

```bash
./mvnw test
```

> Os testes usam **H2 in-memory** automaticamente — nenhum banco externo necessário.

---

## 🏗️ Estrutura do Projeto

```
src/main/java/br/com/oficina/
├── auth/                         # Bounded context de autenticação
│   ├── domain/model/             # Entidade Usuario
│   └── infrastructure/
│       ├── controller/           # AuthController + DTOs
│       ├── repository/           # UsuarioRepository (JPA)
│       ├── security/             # JWT Filter, JwtUtil, SecurityConfig, UserDetailsService
│       └── config/               # SwaggerConfig
├── ordemservico/                 # Bounded context principal (OS, Clientes, Veículos)
│   ├── application/              # Use Cases e Services
│   ├── domain/                   # Entidades, Repositórios (interfaces), Exceções
│   └── infrastructure/           # Controllers, Persistence (in-memory)
└── OficinaApplication.java
```
