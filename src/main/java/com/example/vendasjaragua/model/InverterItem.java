package com.example.vendasjaragua.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InverterItem implements Serializable {
    private String fabricante;
    private Double potencia;
    private Integer quantidade;
}
