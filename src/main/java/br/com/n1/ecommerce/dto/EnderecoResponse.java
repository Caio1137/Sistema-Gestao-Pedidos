package br.com.n1.ecommerce.dto;

import br.com.n1.ecommerce.model.Endereco;

public record EnderecoResponse(
    Long id,
    String rua,
    String cidade,
    String cep
) {

    public static EnderecoResponse fromEntity(Endereco endereco) {
        return new EnderecoResponse(
            endereco.getId(),
            endereco.getRua(),
            endereco.getCidade(),
            endereco.getCep()
        );
    }
}

