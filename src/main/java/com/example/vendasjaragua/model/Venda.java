package com.example.vendasjaragua.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vendas_jaragua")
@Data
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cliente;
    private String nf;
    private String ov;
    private String entrega; // Could be date or text
    private String telefone;
    private String cidade;
    private String estado;
    private String vendedor;

    @Column(name = "data_venda")
    private LocalDate data;

    private String placas;
    private String inversor;
    private String potencia; 

    @Column(name = "valor_venda")
    private BigDecimal valorVenda;

    @Column(name = "valor_material")
    private BigDecimal valorMaterial;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<VendaItem> produto; // Kept name 'produto' as requested, but now it is a List

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inverter_info", columnDefinition = "jsonb")
    private List<InverterItem> inverterInfo = new ArrayList<>();

    private String time;

    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        if (produto != null && !produto.isEmpty()) {
            this.valorVenda = produto.stream()
                .filter(i -> i.getValorUnitarioVenda() != null && i.getQuantidade() != null)
                .map(i -> i.getValorUnitarioVenda().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.valorMaterial = produto.stream()
                .filter(i -> i.getValorUnitarioCusto() != null && i.getQuantidade() != null)
                .map(i -> i.getValorUnitarioCusto().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public BigDecimal getValorBruto() {
        if (valorVenda != null && valorMaterial != null) {
            return valorVenda.subtract(valorMaterial);
        }
        return null;
    }

    public Double getMarkup() {
        if (valorVenda != null && valorMaterial != null && valorMaterial.compareTo(BigDecimal.ZERO) != 0) {
            try {
                // (valorVenda / valorMaterial) - 1
                return valorVenda.divide(valorMaterial, 4, java.math.RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE)
                        .doubleValue();
            } catch (ArithmeticException e) {
                return 0.0;
            }
        }
        return null;
    }
}
