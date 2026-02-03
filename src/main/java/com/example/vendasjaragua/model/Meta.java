package com.example.vendasjaragua.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "metas_jaragua", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"referencia", "tipo", "time_id"})
})
@Data
public class Meta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate referencia; // Always 1st of month

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMeta tipo; // GLOBAL or TIME

    @ManyToOne
    @JoinColumn(name = "time_id")
    private Time time; // Null if tipo == GLOBAL

    private BigDecimal valor;

    public enum TipoMeta {
        GLOBAL, TIME
    }
}
