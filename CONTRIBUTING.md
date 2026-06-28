# Guia de Contribuição — Oficina API

Este documento registra, de forma rastreável no repositório, as regras de processo que **DEVEM**
ser seguidas em qualquer contribuição. A governança completa vive na constituição do projeto
(`.specify/memory/constitution.md`); este guia destaca os pontos que afetam diretamente cada PR.

## Regra inegociável: documentação sincronizada com endpoints do domínio

> **Toda nova feature ou mudança que crie, altere ou remova um endpoint do domínio DEVE atualizar,
> no mesmo PR, TODA a documentação afetada.** (Constituição, Princípio XIII.)

Sempre que um endpoint mudar, o PR DEVE atualizar, na mesma entrega:

1. **Swagger / OpenAPI** — a documentação do endpoint criado/alterado/removido
   (anotações nos `*ControllerSwagger`), refletindo path, método, request/response e autenticação.
2. **README do módulo** afetado — `src/main/java/br/com/oficina/<modulo>/README.md`: tabela de
   endpoints, máquina de estados e ciclo de vida, quando impactados.
3. **Collection do Insomnia** — `docs/collections/oficina-api.insomnia.json`: **gerada** a partir do
   OpenAPI pelo script, não editada à mão. Regere e commite com:

   ```sh
   # app rodando com /v3/api-docs acessível:
   python3 scripts/gerar-collection-insomnia.py
   # ou a partir de um OpenAPI exportado:
   python3 scripts/gerar-collection-insomnia.py --input openapi.json
   ```

   A collection traz apenas o **Base Environment** com placeholders (sem segredos). Cada
   desenvolvedor preenche um **Sub Environment local** no Insomnia (não versionado); reimportar uma
   versão atualizada altera só os endpoints afetados e preserva esses valores locais. Os testes do
   script rodam manualmente (`python3 -m unittest discover -s scripts/tests`), fora do `./mvnw verify`.
4. **Documentação técnica descritiva** — `docs/contexto-tecnico.md` (máquina de estados/endpoints),
   quando aplicável.

Pontos de atenção:

- **Remover um endpoint** significa remover suas referências dos artefatos acima — não basta marcar
  como "legado".
- Nenhum PR que toque um endpoint pode ser mergeado com documentação desatualizada. Divergência
  entre código e documentação (rótulos invertidos, rotas mortas) é tratada como violação de
  governança e DEVE ser corrigida antes do merge.

## Checklist mínimo de PR

Antes de abrir/mergear um PR, confirme:

- [ ] Endpoints novos/alterados/removidos refletidos no **Swagger/OpenAPI**
- [ ] **README do módulo** afetado atualizado (endpoints, estados, ciclo de vida)
- [ ] **Collection do Insomnia** atualizada e com JSON válido
- [ ] `docs/contexto-tecnico.md` realinhado quando a máquina de estados/endpoints mudou
- [ ] Testes presentes e `./mvnw verify` verde (cobertura ≥ 80% nos módulos afetados)
- [ ] Migração Flyway `V{N}__descricao.sql` para mudanças de schema (nunca `ddl-auto`)
- [ ] Commits em pt-BR seguindo Conventional Commits 1.0.0

> A lista completa de princípios e o checklist de Code Review estão na constituição
> (`.specify/memory/constitution.md`, seção *Governance*).
