package br.com.n1.ecommerce.dto;

import br.com.n1.ecommerce.model.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record PedidoStatusUpdateRequest(
    @NotNull(message = "Status e obrigatorio")
    StatusPedido status
) {
}

