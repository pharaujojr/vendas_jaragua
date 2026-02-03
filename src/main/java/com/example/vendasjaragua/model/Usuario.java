package com.example.vendasjaragua.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "usuarios_vendas_jaragua")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    private String username;
    private String password;
    private String role;
}
