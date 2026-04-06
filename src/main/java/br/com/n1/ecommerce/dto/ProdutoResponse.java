package br.com.n1.ecommerce.dto;

import java.math.BigDecimal;

import br.com.n1.ecommerce.model.Produto;

public record ProdutoResponse(
    Long id,
    String nome,
    BigDecimal preco,
    Integer estoque
) {

    public static ProdutoResponse fromEntity(Produto produto) {
        return new ProdutoResponse(
            produto.getId(),
            produto.getNome(),
            produto.getPreco(),
            produto.getEstoque()
        );
    }
}

