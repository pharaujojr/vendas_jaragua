-- Script para criar a tabela de usuários
-- Execute este script no seu banco de dados PostgreSQL

CREATE TABLE IF NOT EXISTS usuarios_vendas_jaragua (
    username VARCHAR(100) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- O usuário padrão 'solturi' será criado automaticamente pela aplicação ao iniciar se não existir.
-- Mas se quiser criar manualmente, a senha 'Solturi2025.' em BCrypt é algo similar a:
-- INSERT INTO usuarios_vendas_jaragua (username, password, role) 
-- VALUES ('solturi', '$2a$10$X7X...', 'ADMIN') ON CONFLICT DO NOTHING;
