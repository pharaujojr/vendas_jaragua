package com.example.vendasjaragua.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "jaragua_produtos")
@Data
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private String grupo;
    private String unidade;
}
