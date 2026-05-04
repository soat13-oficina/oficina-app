UPDATE funcionarios
   SET cpf = regexp_replace(cpf, '[^0-9]', '', 'g')
 WHERE cpf IS NOT NULL
   AND cpf ~ '[^0-9]';

ALTER TABLE funcionarios
    ADD CONSTRAINT uq_funcionarios_cpf UNIQUE (cpf);
