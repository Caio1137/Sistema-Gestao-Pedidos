package br.com.n1.ecommerce.dto;

import br.com.n1.ecommerce.model.Produto;

public record ProdutoResumoResponse(
    Long id,
    String nome
) {

    public static ProdutoResumoResponse fromEntity(Produto produto) {
        return new ProdutoResumoResponse(produto.getId(), produto.getNome());
    }
}

