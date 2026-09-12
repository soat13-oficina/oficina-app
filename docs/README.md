# Documentação — Oficina API

Diagramas, decisões arquiteturais e material de apoio do projeto.

## Documentação da arquitetura

Entrada única: **[`arquitetura/README.md`](arquitetura/README.md)** — mapeia cada
exigência do enunciado ao documento correspondente.

| Pasta / arquivo | Conteúdo |
|---|---|
| [`arquitetura/diagrama-componentes.md`](arquitetura/diagrama-componentes.md) | Visão de nuvem: componentes, APIs, banco, monitoramento e a cadeia de provisionamento entre os quatro repositórios |
| [`arquitetura/diagramas-sequencia.md`](arquitetura/diagramas-sequencia.md) | Autenticação por CPF, consumo de rota protegida e abertura de ordem de serviço |
| [`arquitetura/modelo-de-dados.md`](arquitetura/modelo-de-dados.md) | Diagrama ER, explicação dos relacionamentos e ajustes do modelo relacional |
| [`rfc/`](rfc/) | RFCs: escolha da nuvem, do banco de dados e da estratégia de autenticação |
| [`adr/`](adr/) | ADRs da aplicação: estratégia de escala e snapshot em OS/orçamentos |

## Diagramas e apoio

| Arquivo | Conteúdo |
|---|---|
| `fluxograma.md` | Arquitetura hexagonal, ciclo de vida da OS e do Orçamento, módulos e autenticação |
| `teste-funcional.md` | Passo a passo de teste funcional completo da API |
| `observabilidade.md` | Métricas de negócio, logs estruturados, correlação e como conferir tudo localmente |
| `modelo_readme.md` | Modelo de referência para os READMEs de domínio em `src/main/java/br/com/oficina/*/` |
| `collections/oficina-api.insomnia.json` | Collection do Insomnia **gerada** a partir do OpenAPI (ver abaixo) |

---

## Collection do Insomnia (gerada)

A collection `collections/oficina-api.insomnia.json` é **gerada** a partir do contrato OpenAPI da
aplicação — não edite à mão. Para regerar:

```sh
python3 scripts/gerar-collection-insomnia.py            # busca /v3/api-docs (app rodando)
python3 scripts/gerar-collection-insomnia.py --input openapi.json   # a partir de um arquivo
```

Como usar no Insomnia preservando sua configuração local:

1. Importe `collections/oficina-api.insomnia.json` (traz o **Base Environment** só com placeholders).
2. Crie um **Sub Environment local** (ex.: "Meu Local") e preencha os valores reais (`token`,
   ids, etc.). Esse Sub Environment **não é versionado**.
3. Em atualizações futuras, reimporte a collection: apenas os endpoints afetados mudam e os valores
   do seu Sub Environment local são preservados (não é preciso realimentar variáveis).

Os testes do gerador rodam com `python3 -m unittest discover -s scripts/tests`.

---

## Como visualizar os diagramas

### Opção 1 — VS Code (recomendado)

Instale a extensão **Markdown Preview Mermaid Support**:

1. Abra o painel de extensões (`Ctrl+Shift+X` / `Cmd+Shift+X`)
2. Pesquise por `Markdown Preview Mermaid Support` (publicado por *Matt Bierner*)
3. Instale e recarregue o VS Code
4. Abra o arquivo `.md` e pressione `Ctrl+Shift+V` / `Cmd+Shift+V` para abrir o preview

### Opção 2 — Mermaid Live Editor (online, sem instalação)

1. Acesse **https://mermaid.live**
2. Abra o arquivo desejado (`fluxograma.md` ou `teste-funcional.md`)
3. Copie o conteúdo de dentro do bloco ` ```mermaid ... ``` ` (apenas o conteúdo, sem as marcações)
4. Cole no painel esquerdo do editor — o diagrama renderiza automaticamente à direita

### Opção 3 — IntelliJ IDEA

1. Instale o plugin **Mermaid** (Settings → Plugins → Marketplace → "Mermaid")
2. Abra o arquivo `.md` e clique na aba **Preview** no canto superior direito do editor

### Opção 4 — GitHub / GitLab

Faça push para o repositório remoto — ambas as plataformas renderizam blocos Mermaid nativamente em arquivos `.md`.

---

## Dica

Cada diagrama no `fluxograma.md` é independente. Ao usar o Mermaid Live Editor, copie um diagrama por vez (cada bloco ` ```mermaid ``` ` separadamente).
