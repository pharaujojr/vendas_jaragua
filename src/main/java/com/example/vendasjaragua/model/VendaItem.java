package com.example.vendasjaragua.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VendaItem {
    private Long produtoId;
    private String nomeProduto; // storing name for historical accuracy or ease of display
    private Integer quantidade;
    private BigDecimal valorUnitarioVenda; // unit price for sale
    private BigDecimal valorUnitarioCusto; // unit cost (material)
    private String grupo;
}
