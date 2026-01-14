package com.example.vendasjaragua.repository;

import com.example.vendasjaragua.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Venda v WHERE v.cliente IS NULL AND v.vendedor IS NULL AND v.data IS NULL AND v.valorVenda IS NULL")
    void deleteEmptyRows();

    Page<Venda> findByDataBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
