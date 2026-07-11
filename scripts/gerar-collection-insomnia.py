#!/usr/bin/env python3
"""Gera a collection do Insomnia a partir do contrato OpenAPI da aplicacao.

A collection e gerada de forma DETERMINISTICA com identificadores estaveis por
endpoint, de modo que reimporta-la no Insomnia altere apenas os endpoints
afetados (o Insomnia reconcilia recursos por `_id`). O Base Environment contem
apenas nomes/placeholders de variaveis (sem segredos); os valores reais vivem
num Sub Environment local do desenvolvedor, preservado entre reimportacoes.

Uso:
    python3 scripts/gerar-collection-insomnia.py [--input openapi.json]
        [--base-url http://localhost:8080]
        [--output docs/collections/oficina-api.insomnia.json]

Fonte do OpenAPI: --input (arquivo) ou, na ausencia, GET em
<base-url>/v3/api-docs (SpringDoc). Ver specs/013-insomnia-merge-incremental/.
"""

import argparse
import json
import os
import re
import sys
import urllib.request

EXPORT_FORMAT = 4
EXPORT_SOURCE = "oficina:gerar-collection-insomnia"
WORKSPACE_ID = "wrk_oficina"
ENV_BASE_ID = "env_base"
DEFAULT_OUTPUT = "docs/collections/oficina-api.insomnia.json"
DEFAULT_BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")

# Substrings que marcam uma variavel como sensivel: seu valor NUNCA e versionado.
PADROES_SENSIVEIS = ("token", "secret", "password", "senha", "authorization", "apikey", "api_key")

# Defaults nao sensiveis uteis no Base Environment (placeholders de exemplo).
DEFAULTS_NAO_SENSIVEIS = {"base_url": ""}  # base_url e preenchido com a base_url efetiva


class ErroGeracao(Exception):
    """Falha de geracao com mensagem clara para o usuario."""


def slug(texto):
    """Normaliza um texto para um identificador estavel: minusculas, nao
    alfanumericos viram '_', colapsa e apara '_' das pontas."""
    s = re.sub(r"[^a-zA-Z0-9]+", "_", texto).strip("_").lower()
    s = re.sub(r"_+", "_", s)
    return s


def e_sensivel(nome):
    nome_baixo = nome.lower()
    return any(p in nome_baixo for p in PADROES_SENSIVEIS)


def carregar_openapi(input_path, base_url):
    """Carrega o OpenAPI de arquivo (input_path) ou via GET <base_url>/v3/api-docs."""
    try:
        if input_path:
            with open(input_path, "r", encoding="utf-8") as f:
                dado = json.load(f)
        else:
            url = base_url.rstrip("/") + "/v3/api-docs"
            with urllib.request.urlopen(url, timeout=15) as resp:
                dado = json.loads(resp.read().decode("utf-8"))
    except FileNotFoundError:
        raise ErroGeracao(f"Arquivo OpenAPI nao encontrado: {input_path}")
    except (json.JSONDecodeError, ValueError):
        raise ErroGeracao("OpenAPI invalido: conteudo nao e JSON valido.")
    except OSError as exc:
        raise ErroGeracao(f"Nao foi possivel obter o OpenAPI: {exc}")

    if not isinstance(dado, dict) or "paths" not in dado or not isinstance(dado["paths"], dict):
        raise ErroGeracao("OpenAPI invalido: campo 'paths' ausente ou malformado.")
    if "openapi" not in dado and "swagger" not in dado:
        raise ErroGeracao("OpenAPI invalido: faltam 'openapi'/'swagger' (versao nao suportada).")
    return dado


def exige_auth(operacao, seguranca_global):
    """Determina se a operacao exige autenticacao (Bearer)."""
    if "security" in operacao:
        return bool(operacao["security"])  # [] explicito = publico
    return bool(seguranca_global)


def variavel_de_param(nome_param):
    return slug(nome_param)


def construir(openapi, base_url):
    """Monta a lista de resources da collection a partir do OpenAPI."""
    seguranca_global = openapi.get("security")
    metodos_http = ["get", "post", "put", "patch", "delete", "head", "options"]

    variaveis = {"base_url"}  # sempre presente
    pastas = {}              # id -> resource
    requests = []
    ids_vistos = set()

    def registrar_id(novo_id, contexto):
        if novo_id in ids_vistos:
            raise ErroGeracao(
                f"Colisao de identificador '{novo_id}' ao gerar {contexto}. "
                "Dois recursos derivariam o mesmo _id."
            )
        ids_vistos.add(novo_id)

    registrar_id(WORKSPACE_ID, "workspace")
    registrar_id(ENV_BASE_ID, "base environment")

    for caminho in sorted(openapi["paths"].keys()):
        item = openapi["paths"][caminho]
        for metodo in metodos_http:
            if metodo not in item:
                continue
            operacao = item[metodo]
            tag = (operacao.get("tags") or ["default"])[0]
            pasta_id = "fld_" + slug(tag)
            if pasta_id not in pastas:
                registrar_id(pasta_id, f"pasta '{tag}'")
                pastas[pasta_id] = {
                    "_id": pasta_id,
                    "_type": "request_group",
                    "parentId": WORKSPACE_ID,
                    "name": tag,
                }

            req_id = "req_" + slug(metodo + "_" + caminho)
            registrar_id(req_id, f"request {metodo.upper()} {caminho}")

            # URL com base_url e path params como variaveis
            url_path = caminho
            for nome_param in re.findall(r"\{([^}]+)\}", caminho):
                var = variavel_de_param(nome_param)
                variaveis.add(var)
                url_path = url_path.replace("{" + nome_param + "}", "{{ _." + var + " }}")
            url = "{{ _.base_url }}" + url_path

            headers = []
            if exige_auth(operacao, seguranca_global):
                variaveis.add("token")
                headers.append({"name": "Authorization", "value": "Bearer {{ _.token }}"})

            body = {}
            req_body = operacao.get("requestBody", {})
            conteudo = (req_body.get("content") or {}).get("application/json")
            if conteudo is not None:
                headers.append({"name": "Content-Type", "value": "application/json"})
                exemplo = conteudo.get("example")
                if exemplo is None and isinstance(conteudo.get("examples"), dict):
                    for v in conteudo["examples"].values():
                        if isinstance(v, dict) and "value" in v:
                            exemplo = v["value"]
                            break
                texto = json.dumps(exemplo, ensure_ascii=False, indent=2) if exemplo is not None else "{}"
                body = {"mimeType": "application/json", "text": texto}

            requests.append({
                "_id": req_id,
                "_type": "request",
                "parentId": pasta_id,
                "name": operacao.get("summary") or f"{metodo.upper()} {caminho}",
                "method": metodo.upper(),
                "url": url,
                "body": body,
                "headers": headers,
                "_sort_key": (pasta_id, metodos_http.index(metodo), caminho),
            })

    # Base Environment: nomes das variaveis com placeholders (sensiveis = "")
    data = {}
    for nome in sorted(variaveis):
        if nome == "base_url":
            data[nome] = base_url
        elif e_sensivel(nome):
            data[nome] = ""
        else:
            data[nome] = ""

    # Guarda anti-segredo: nenhuma variavel sensivel pode ter valor versionado
    for nome, valor in data.items():
        if e_sensivel(nome) and valor:
            raise ErroGeracao(
                f"Variavel sensivel '{nome}' nao pode ter valor no arquivo versionado."
            )

    env_base = {
        "_id": ENV_BASE_ID,
        "_type": "environment",
        "parentId": WORKSPACE_ID,
        "name": "Base (placeholders — preencha um Sub Environment local)",
        "data": data,
    }

    workspace = {
        "_id": WORKSPACE_ID,
        "_type": "workspace",
        "name": "Oficina API",
        "description": "Collection gerada a partir do OpenAPI (não editar à mão).",
        "scope": "collection",
    }

    # Ordenacao deterministica: workspace, env, pastas (por id), requests (por sort key)
    resources = [workspace, env_base]
    resources.extend(pastas[pid] for pid in sorted(pastas))
    for req in sorted(requests, key=lambda r: r["_sort_key"]):
        req.pop("_sort_key", None)
        resources.append(req)
    return resources


def gerar(input_path, base_url, output_path):
    openapi = carregar_openapi(input_path, base_url)
    resources = construir(openapi, base_url)
    collection = {
        "_type": "export",
        "__export_format": EXPORT_FORMAT,
        "__export_source": EXPORT_SOURCE,
        "resources": resources,
    }
    saida = json.dumps(collection, ensure_ascii=False, indent=2) + "\n"
    if output_path == "-":
        sys.stdout.write(saida)
    else:
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(saida)
    return collection


def main(argv=None):
    parser = argparse.ArgumentParser(description="Gera a collection do Insomnia a partir do OpenAPI.")
    parser.add_argument("--input", default=None, help="Caminho de um OpenAPI JSON local.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL,
                        help="Base para buscar /v3/api-docs quando --input nao e informado.")
    parser.add_argument("--output", default=DEFAULT_OUTPUT,
                        help="Arquivo de saida (use '-' para stdout).")
    args = parser.parse_args(argv)
    try:
        gerar(args.input, args.base_url, args.output)
    except ErroGeracao as exc:
        print(f"erro: {exc}", file=sys.stderr)
        return 1
    if args.output != "-":
        print(f"collection gerada em {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
