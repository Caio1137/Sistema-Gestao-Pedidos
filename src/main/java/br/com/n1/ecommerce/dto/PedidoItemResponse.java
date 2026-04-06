package br.com.n1.ecommerce.dto;

import java.math.BigDecimal;

import br.com.n1.ecommerce.model.ItemPedido;

public record PedidoItemResponse(
    Long id,
    Integer quantidade,
    BigDecimal precoUnitario,
    BigDecimal subtotal,
    ProdutoResumoResponse produto
) {

    public static PedidoItemResponse fromEntity(ItemPedido itemPedido) {
        return new PedidoItemResponse(
            itemPedido.getId(),
            itemPedido.getQuantidade(),
            itemPedido.getPrecoUnitario(),
            itemPedido.getSubtotal(),
            ProdutoResumoResponse.fromEntity(itemPedido.getProduto())
        );
    }
}

