package com.example.vendasjaragua.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "jaragua_time")
@Data
public class Time {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String lider;

    @OneToMany(mappedBy = "time")
    private List<Vendedor> vendedores;
}
