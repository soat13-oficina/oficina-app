-- Status do cliente, consultado pela Function Serverless de autenticacao
-- (repositorio oficina-lambda-auth) para decidir entre emitir o token e
-- recusar com 403 CLIENTE_INATIVO.
--
-- Ate aqui o modelo nao tinha nenhuma nocao de status: um cliente existia ou
-- nao existia. Isso tornava impossivel atender "consultar a existencia E o
-- status do cliente" sem redefinir status como existencia.
--
-- NOT NULL DEFAULT TRUE: todo cliente ja cadastrado passa a ser ativo, que e o
-- comportamento vigente hoje - a migration nao muda o significado de nenhum
-- dado existente.
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Indice funcional sobre o documento normalizado.
--
-- A coluna cpf_ou_cnpj guarda o documento COMO FOI DIGITADO: o dominio valida
-- por digito verificador, mas persiste a string original, entao o mesmo CPF
-- pode estar gravado como "529.982.247-25" ou "52998224725". A Lambda de
-- autenticacao busca por digitos e precisa comparar sobre a forma normalizada:
--
--   WHERE regexp_replace(cpf_ou_cnpj, '\D', '', 'g') = $1
--
-- Sem este indice a comparacao e sobre uma expressao e forca sequential scan,
-- ignorando uk_clientes_cpf_ou_cnpj. regexp_replace e IMMUTABLE, o que permite
-- indexa-la.
--
-- NOTA: este indice NAO e unico de proposito. Tornar unico aqui abortaria a
-- migration em qualquer base onde o mesmo documento exista com e sem mascara -
-- limitacao real de uk_clientes_cpf_ou_cnpj, que compara a string bruta e
-- portanto nao impede esse duplicado. Normalizar na escrita e desduplicar os
-- dados existentes e um passo a parte, com decisao de negocio envolvida.
CREATE INDEX IF NOT EXISTS idx_clientes_documento_digitos
    ON clientes ((regexp_replace(cpf_ou_cnpj, '\D', '', 'g')));
