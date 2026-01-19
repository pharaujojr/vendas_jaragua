package com.example.vendasjaragua.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class VendaRepositoryImpl implements VendaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Map<String, Object>> findFaturamentoPorGrupoDynamic(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> times,
            List<String> vendedores,
            List<String> grupos,
            List<String> produtos) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(p.grupo, 'Não Especificado') as grupo, ");
        sql.append("SUM(CAST(COALESCE(NULLIF(item->>'valorUnitarioVenda', ''), '0') AS NUMERIC) * ");
        sql.append("CAST(COALESCE(NULLIF(item->>'quantidade', ''), '0') AS INTEGER)) as total ");
        sql.append("FROM vendas_jaragua v ");
        sql.append("CROSS JOIN jsonb_array_elements(COALESCE(v.produto, CAST('[]' AS jsonb))) AS item ");
        sql.append("LEFT JOIN jaragua_produtos p ON item->>'nomeProduto' = p.descricao ");
        sql.append("WHERE v.data_venda BETWEEN :dataInicio AND :dataFim ");
        
        if (times != null && !times.isEmpty()) {
            sql.append("AND v.time IN (:times) ");
        }
        if (vendedores != null && !vendedores.isEmpty()) {
            sql.append("AND v.vendedor IN (:vendedores) ");
        }
        if (grupos != null && !grupos.isEmpty()) {
            sql.append("AND (p.grupo IN (:grupos) OR ");
            sql.append("(COALESCE(p.grupo, '') = '' AND 'Não Especificado' IN (:grupos))) ");
        }
        if (produtos != null && !produtos.isEmpty()) {
            sql.append("AND item->>'nomeProduto' IN (:produtos) ");
        }
        
        sql.append("GROUP BY grupo ");
        sql.append("ORDER BY total DESC");
        
        var query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dataInicio", dataInicio);
        query.setParameter("dataFim", dataFim);
        
        if (times != null && !times.isEmpty()) query.setParameter("times", times);
        if (vendedores != null && !vendedores.isEmpty()) query.setParameter("vendedores", vendedores);
        if (grupos != null && !grupos.isEmpty()) query.setParameter("grupos", grupos);
        if (produtos != null && !produtos.isEmpty()) query.setParameter("produtos", produtos);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("grupo", row[0] == null ? "Não Especificado" : row[0]);
            map.put("total", row[1]);
            mapped.add(map);
        }
        return mapped;
    }

    @Override
    public List<Map<String, Object>> findVendasPorMes(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> times,
            List<String> vendedores,
            List<String> grupos,
            List<String> produtos) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TO_CHAR(v.data_venda, 'YYYY-MM') as mes, ");
        sql.append("SUM(CAST(COALESCE(NULLIF(item->>'valorUnitarioVenda', ''), '0') AS NUMERIC) * ");
        sql.append("CAST(COALESCE(NULLIF(item->>'quantidade', ''), '0') AS INTEGER)) as total ");
        sql.append("FROM vendas_jaragua v ");
        sql.append("CROSS JOIN jsonb_array_elements(COALESCE(v.produto, CAST('[]' AS jsonb))) AS item ");
        sql.append("LEFT JOIN jaragua_produtos p ON item->>'nomeProduto' = p.descricao ");
        sql.append("WHERE v.data_venda BETWEEN :dataInicio AND :dataFim ");
        
        if (times != null && !times.isEmpty()) {
            sql.append("AND v.time IN (:times) ");
        }
        if (vendedores != null && !vendedores.isEmpty()) {
            sql.append("AND v.vendedor IN (:vendedores) ");
        }
        if (grupos != null && !grupos.isEmpty()) {
            sql.append("AND (p.grupo IN (:grupos) OR ");
            sql.append("(COALESCE(p.grupo, '') = '' AND 'Não Especificado' IN (:grupos))) ");
        }
        if (produtos != null && !produtos.isEmpty()) {
            sql.append("AND item->>'nomeProduto' IN (:produtos) ");
        }
        
        sql.append("GROUP BY mes ");
        sql.append("ORDER BY mes");
        
        var query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dataInicio", dataInicio);
        query.setParameter("dataFim", dataFim);
        
        if (times != null && !times.isEmpty()) query.setParameter("times", times);
        if (vendedores != null && !vendedores.isEmpty()) query.setParameter("vendedores", vendedores);
        if (grupos != null && !grupos.isEmpty()) query.setParameter("grupos", grupos);
        if (produtos != null && !produtos.isEmpty()) query.setParameter("produtos", produtos);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("mes", row[0]);
            map.put("total", row[1]);
            mapped.add(map);
        }
        return mapped;
    }

    @Override
    public long countVendasFiltradas(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> times,
            List<String> vendedores,
            List<String> grupos,
            List<String> produtos) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT v.id) FROM vendas_jaragua v ");
        
        // Se filtrar grupos ou produtos, precisa do CROSS JOIN
        if ((grupos != null && !grupos.isEmpty()) || (produtos != null && !produtos.isEmpty())) {
            sql.append("CROSS JOIN jsonb_array_elements(COALESCE(v.produto, CAST('[]' AS jsonb))) AS item ");
            sql.append("LEFT JOIN jaragua_produtos p ON item->>'nomeProduto' = p.descricao ");
        }
        
        sql.append("WHERE v.data_venda BETWEEN :dataInicio AND :dataFim ");
        
        if (times != null && !times.isEmpty()) {
            sql.append("AND v.time IN (:times) ");
        }
        if (vendedores != null && !vendedores.isEmpty()) {
            sql.append("AND v.vendedor IN (:vendedores) ");
        }
        if (grupos != null && !grupos.isEmpty()) {
            sql.append("AND (p.grupo IN (:grupos) OR ");
            sql.append("(COALESCE(p.grupo, '') = '' AND 'Não Especificado' IN (:grupos))) ");
        }
        if (produtos != null && !produtos.isEmpty()) {
            sql.append("AND item->>'nomeProduto' IN (:produtos) ");
        }
        
        var query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dataInicio", dataInicio);
        query.setParameter("dataFim", dataFim);
        
        if (times != null && !times.isEmpty()) query.setParameter("times", times);
        if (vendedores != null && !vendedores.isEmpty()) query.setParameter("vendedores", vendedores);
        if (grupos != null && !grupos.isEmpty()) query.setParameter("grupos", grupos);
        if (produtos != null && !produtos.isEmpty()) query.setParameter("produtos", produtos);
        
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<Map<String, Object>> findVendasPorVendedorDynamic(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> times,
            List<String> vendedores,
            List<String> grupos,
            List<String> produtos) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT v.vendedor, ");
        sql.append("SUM(CAST(COALESCE(NULLIF(item->>'valorUnitarioVenda', ''), '0') AS NUMERIC) * ");
        sql.append("CAST(COALESCE(NULLIF(item->>'quantidade', ''), '0') AS INTEGER)) as total ");
        sql.append("FROM vendas_jaragua v ");
        sql.append("CROSS JOIN jsonb_array_elements(COALESCE(v.produto, CAST('[]' AS jsonb))) AS item ");
        sql.append("LEFT JOIN jaragua_produtos p ON item->>'nomeProduto' = p.descricao ");
        sql.append("WHERE v.data_venda BETWEEN :dataInicio AND :dataFim ");
        
        if (times != null && !times.isEmpty()) {
            sql.append("AND v.time IN (:times) ");
        }
        if (vendedores != null && !vendedores.isEmpty()) {
            sql.append("AND v.vendedor IN (:vendedores) ");
        }
        if (grupos != null && !grupos.isEmpty()) {
            sql.append("AND (p.grupo IN (:grupos) OR ");
            sql.append("(COALESCE(p.grupo, '') = '' AND 'Não Especificado' IN (:grupos))) ");
        }
        if (produtos != null && !produtos.isEmpty()) {
            sql.append("AND item->>'nomeProduto' IN (:produtos) ");
        }
        
        sql.append("GROUP BY v.vendedor ");
        sql.append("ORDER BY total DESC");
        
        var query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dataInicio", dataInicio);
        query.setParameter("dataFim", dataFim);
        
        if (times != null && !times.isEmpty()) query.setParameter("times", times);
        if (vendedores != null && !vendedores.isEmpty()) query.setParameter("vendedores", vendedores);
        if (grupos != null && !grupos.isEmpty()) query.setParameter("grupos", grupos);
        if (produtos != null && !produtos.isEmpty()) query.setParameter("produtos", produtos);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("vendedor", row[0]);
            map.put("total", row[1]);
            mapped.add(map);
        }
        return mapped;
    }
}
