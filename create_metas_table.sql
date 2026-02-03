-- Script para criar a tabela de histórico de metas
-- Execute este script no seu banco de dados PostgreSQL

CREATE TABLE IF NOT EXISTS metas_jaragua (
    id BIGSERIAL PRIMARY KEY,
    referencia DATE NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(19, 2),
    time_id BIGINT,
    
    CONSTRAINT fk_meta_time 
        FOREIGN KEY (time_id) 
        REFERENCES jaragua_time(id),
        
    -- Garante que não haja duplicidade de meta para o mesmo time/tipo no mesmo mês
    CONSTRAINT uk_meta_ref_tipo_time 
        UNIQUE (referencia, tipo, time_id)
);

-- Comentário: O campo time_id pode ser nulo se o tipo for 'GLOBAL'
-- Se o tipo for 'TIME', o time_id deve ser preenchido (embora o banco permita nulo, a validação é feita na aplicação)
