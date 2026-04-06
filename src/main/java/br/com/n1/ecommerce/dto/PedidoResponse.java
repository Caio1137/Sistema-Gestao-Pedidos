package br.com.n1.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.n1.ecommerce.model.Pedido;
import br.com.n1.ecommerce.model.StatusPedido;

public record PedidoResponse(
    Long id,
    LocalDateTime data,
    StatusPedido status,
    BigDecimal total,
    ClienteResumoResponse cliente,
    EnderecoResponse endereco,
    List<PedidoItemResponse> itens
) {

    public static PedidoResponse fromEntity(Pedido pedido) {
        return new PedidoResponse(
            pedido.getId(),
            pedido.getData(),
            pedido.getStatus(),
            pedido.getTotal(),
            ClienteResumoResponse.fromEntity(pedido.getCliente()),
            EnderecoResponse.fromEntity(pedido.getEnderecoEntrega()),
            pedido.getItens().stream()
                .map(PedidoItemResponse::fromEntity)
                .toList()
        );
    }
}

