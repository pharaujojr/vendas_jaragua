package com.example.vendasjaragua.repository;

import com.example.vendasjaragua.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetaRepository extends JpaRepository<Meta, Long> {
    List<Meta> findByReferencia(LocalDate referencia);
    Optional<Meta> findByReferenciaAndTipoAndTimeId(LocalDate referencia, Meta.TipoMeta tipo, Long timeId);
    Optional<Meta> findByReferenciaAndTipo(LocalDate referencia, Meta.TipoMeta tipo);
}
