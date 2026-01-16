package com.example.vendasjaragua.repository;

import com.example.vendasjaragua.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query("SELECT function('to_char', v.data, 'YYYY-MM') as mes, SUM(v.valorVenda) FROM Venda v WHERE v.data BETWEEN :inicio AND :fim AND ((:times) IS NULL OR v.time IN (:times)) AND ((:vendedores) IS NULL OR v.vendedor IN (:vendedores)) GROUP BY function('to_char', v.data, 'YYYY-MM') ORDER BY mes")
    List<Object[]> findVendasPorMes(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("times") List<String> times, @Param("vendedores") List<String> vendedores);

    @Query("SELECT v.vendedor, SUM(v.valorVenda) FROM Venda v WHERE v.data BETWEEN :inicio AND :fim GROUP BY v.vendedor ORDER BY SUM(v.valorVenda) DESC")
    List<Object[]> findVendasPorVendedor(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT v.time, SUM(v.valorVenda) FROM Venda v WHERE v.data BETWEEN :inicio AND :fim GROUP BY v.time ORDER BY SUM(v.valorVenda) DESC")
    List<Object[]> findVendasPorTime(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT v.vendedor, SUM(v.valorVenda) FROM Venda v WHERE v.time = :time AND v.data BETWEEN :inicio AND :fim GROUP BY v.vendedor ORDER BY SUM(v.valorVenda) DESC")
    List<Object[]> findVendasPorVendedorAndTime(@Param("time") String time, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT v.vendedor, SUM(v.valorVenda) FROM Venda v WHERE v.time IS NULL AND v.data BETWEEN :inicio AND :fim GROUP BY v.vendedor ORDER BY SUM(v.valorVenda) DESC")
    List<Object[]> findVendasPorVendedorWhereTimeIsNull(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT function('to_char', v.data, 'YYYY-MM') as mes, v.time, SUM(v.valorVenda) FROM Venda v WHERE v.data BETWEEN :inicio AND :fim GROUP BY function('to_char', v.data, 'YYYY-MM'), v.time ORDER BY mes")
    List<Object[]> findEvolucaoVendasPorTime(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Modifying
    @Transactional
    @Query("DELETE FROM Venda v WHERE v.cliente IS NULL AND v.vendedor IS NULL AND v.data IS NULL AND v.valorVenda IS NULL")
    void deleteEmptyRows();

    Page<Venda> findByDataBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
