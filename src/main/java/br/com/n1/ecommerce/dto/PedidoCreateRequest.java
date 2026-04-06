package br.com.n1.ecommerce.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoCreateRequest(
    @NotNull(message = "Cliente e obrigatorio")
    @Positive(message = "Cliente deve ser um identificador valido")
    Long clienteId,

    @NotNull(message = "Endereco e obrigatorio")
    @Positive(message = "Endereco deve ser um identificador valido")
    Long enderecoId,

    @NotEmpty(message = "Pedido deve ter ao menos um item")
    List<@Valid PedidoItemRequest> itens
) {
}

