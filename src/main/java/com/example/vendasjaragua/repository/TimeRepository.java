package com.example.vendasjaragua.repository;

import com.example.vendasjaragua.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeRepository extends JpaRepository<Time, Long> {
    List<Time> findByNome(String nome);
}
