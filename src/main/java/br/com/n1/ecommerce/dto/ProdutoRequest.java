package br.com.n1.ecommerce.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    String nome,

    @NotNull(message = "Preco e obrigatorio")
    @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero")
    BigDecimal preco,

    @NotNull(message = "Estoque e obrigatorio")
    @PositiveOrZero(message = "Estoque nao pode ser negativo")
    Integer estoque
) {
}

