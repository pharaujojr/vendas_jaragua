-- Script para atualizar o campo 'grupo' dos produtos nas vendas existentes
-- Esse script itera sobre os registros de vendas e atualiza o JSON do campo 'produto'
-- preenchendo o 'grupo' com base no cadastro da tabela jaragua_produtos

-- ATENÇÃO: Execute esse script em um ambiente de teste primeiro!
-- Faça backup antes de executar em produção!

-- Atualizar grupos dos produtos nas vendas
UPDATE vendas_jaragua v
SET produto = (
    SELECT jsonb_agg(
        CASE 
            WHEN p.grupo IS NOT NULL 
            THEN jsonb_set(item, '{grupo}', to_jsonb(p.grupo))
            ELSE item
        END
    )
    FROM jsonb_array_elements(v.produto) AS item
    LEFT JOIN jaragua_produtos p ON p.descricao = item->>'nomeProduto'
)
WHERE EXISTS (
    SELECT 1 
    FROM jsonb_array_elements(v.produto) AS item
    WHERE item->>'grupo' IS NULL OR item->>'grupo' = 'null'
);

-- Verificar quantos registros foram afetados
SELECT 
    COUNT(*) as total_vendas,
    COUNT(CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM jsonb_array_elements(produto) AS item
            WHERE item->>'grupo' IS NULL OR item->>'grupo' = 'null'
        ) THEN 1 
    END) as vendas_com_grupo_null
FROM vendas_jaragua;

-- Mostrar exemplos de produtos sem grupo após a atualização (para verificação)
SELECT 
    v.id,
    v.cliente,
    item->>'nomeProduto' as produto,
    item->>'grupo' as grupo
FROM vendas_jaragua v,
     jsonb_array_elements(v.produto) AS item
WHERE item->>'grupo' IS NULL OR item->>'grupo' = 'null'
LIMIT 10;
