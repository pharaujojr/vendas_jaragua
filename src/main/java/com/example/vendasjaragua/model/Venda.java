package com.example.vendasjaragua.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    private String potencia; // String or number? Assuming String to be safe with formats like "5kWp", or Double. User said "POTÊNCIA". Let's stick to String to avoid parsing errors on import unless it's strictly numeric.

    @Column(name = "valor_venda")
    private BigDecimal valorVenda;

    @Column(name = "valor_material")
    private BigDecimal valorMaterial;

    private String produto;
    private String time;

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
