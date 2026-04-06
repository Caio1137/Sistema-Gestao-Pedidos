package br.com.n1.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoItemRequest(
    @NotNull(message = "Produto e obrigatorio")
    @Positive(message = "Produto deve ser um identificador valido")
    Long produtoId,

    @NotNull(message = "Quantidade e obrigatoria")
    @Positive(message = "Quantidade deve ser maior que zero")
    Integer quantidade
) {
}

