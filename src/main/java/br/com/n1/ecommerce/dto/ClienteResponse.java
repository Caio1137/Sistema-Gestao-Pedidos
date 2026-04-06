package br.com.n1.ecommerce.dto;

import java.util.List;

import br.com.n1.ecommerce.model.Cliente;

public record ClienteResponse(
    Long id,
    String nome,
    String email,
    List<EnderecoResponse> enderecos
) {

    public static ClienteResponse fromEntity(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getNome(),
            cliente.getEmail(),
            cliente.getEnderecos().stream()
                .map(EnderecoResponse::fromEntity)
                .toList()
        );
    }
}

