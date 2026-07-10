#!/usr/bin/env python3
"""Testes do gerador da collection do Insomnia (stdlib unittest).

Rodar: python3 -m unittest discover -s scripts/tests
   ou: python3 scripts/tests/test_gerar_collection.py
"""

import importlib.util
import json
import os
import unittest

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
FIXTURE = os.path.join(AQUI, "openapi-amostra.json")
GOLDEN = os.path.join(AQUI, "collection-esperada.json")

# Carrega o script (nome com hifen) como modulo
_spec = importlib.util.spec_from_file_location(
    "gerar_collection", os.path.join(RAIZ, "scripts", "gerar-collection-insomnia.py"))
gc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gc)


def carregar(caminho):
    with open(caminho, "r", encoding="utf-8") as f:
        return json.load(f)


class TestGeracao(unittest.TestCase):

    def _gerar(self, openapi):
        # Reaproveita o pipeline sem I/O: constroi resources direto
        resources = gc.construir(openapi, "http://localhost:8080")
        return {
            "_type": "export",
            "__export_format": gc.EXPORT_FORMAT,
            "__export_source": gc.EXPORT_SOURCE,
            "resources": resources,
        }

    # US1 — golden file (saida estavel/esperada)
    def test_golden_file(self):
        gerado = self._gerar(carregar(FIXTURE))
        self.assertEqual(gerado, carregar(GOLDEN))

    # US1/FR-011 — determinismo: duas geracoes iguais
    def test_determinismo(self):
        a = self._gerar(carregar(FIXTURE))
        b = self._gerar(carregar(FIXTURE))
        self.assertEqual(
            json.dumps(a, ensure_ascii=False, sort_keys=False),
            json.dumps(b, ensure_ascii=False, sort_keys=False),
        )

    # US2/FR-001 — ids estaveis: request inalterado mantem o mesmo _id mesmo com um endpoint a mais
    def test_ids_estaveis_ao_adicionar_endpoint(self):
        base = carregar(FIXTURE)
        ids_antes = {r["_id"] for r in self._gerar(base)["resources"] if r["_type"] == "request"}

        ampliado = carregar(FIXTURE)
        ampliado["paths"]["/veiculos"] = {
            "get": {"tags": ["Veiculos"], "summary": "Listar veiculos", "responses": {"200": {"description": "ok"}}}
        }
        ids_depois = {r["_id"] for r in self._gerar(ampliado)["resources"] if r["_type"] == "request"}

        self.assertTrue(ids_antes.issubset(ids_depois))
        self.assertIn("req_get_veiculos", ids_depois)

    # US2/FR-006 — colisao de id falha com mensagem clara
    def test_colisao_de_id_falha(self):
        # Dois caminhos que normalizam para o mesmo slug -> mesmo _id
        openapi = {
            "openapi": "3.0.1",
            "paths": {
                "/clientes/ativos": {"get": {"tags": ["C"], "responses": {"200": {"description": "ok"}}}},
                "/clientes-ativos": {"get": {"tags": ["C"], "responses": {"200": {"description": "ok"}}}},
            },
        }
        with self.assertRaises(gc.ErroGeracao):
            gc.construir(openapi, "http://localhost:8080")

    # US3/FR-004 — Base Environment so com placeholders; sensiveis vazios
    def test_base_environment_sem_segredos(self):
        gerado = self._gerar(carregar(FIXTURE))
        env = next(r for r in gerado["resources"] if r["_id"] == gc.ENV_BASE_ID)
        for nome, valor in env["data"].items():
            if gc.e_sensivel(nome):
                self.assertEqual(valor, "", f"variavel sensivel '{nome}' nao pode ter valor")
        self.assertIn("token", env["data"])
        self.assertEqual(env["data"]["token"], "")

    # FR-009 — OpenAPI invalido / formato nao suportado falha com clareza
    def test_openapi_invalido_falha(self):
        with self.assertRaises(gc.ErroGeracao):
            gc.carregar_openapi(os.path.join(AQUI, "nao-existe.json"), "http://localhost:8080")

    def test_openapi_sem_paths_falha(self):
        import tempfile
        with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as f:
            json.dump({"openapi": "3.0.1"}, f)
            caminho = f.name
        try:
            with self.assertRaises(gc.ErroGeracao):
                gc.carregar_openapi(caminho, "http://localhost:8080")
        finally:
            os.unlink(caminho)


if __name__ == "__main__":
    unittest.main()
