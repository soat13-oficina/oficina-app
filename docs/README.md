# Documentação Visual — Oficina API

Esta pasta contém diagramas Mermaid com a arquitetura e os fluxos da aplicação.

| Arquivo | Conteúdo |
|---|---|
| `fluxograma.md` | Arquitetura hexagonal, ciclo de vida da OS e do Orçamento, módulos e autenticação |
| `teste-funcional.md` | Passo a passo de teste funcional completo da API |
| `checklist-validacao-funcional-fase2.md` | Checklist de caixa preta para validar os requisitos de codebase da Fase 2 |

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
