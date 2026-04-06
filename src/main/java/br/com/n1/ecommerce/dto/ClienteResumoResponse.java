package br.com.n1.ecommerce.dto;

import br.com.n1.ecommerce.model.Cliente;

public record ClienteResumoResponse(
    Long id,
    String nome,
    String email
) {

    public static ClienteResumoResponse fromEntity(Cliente cliente) {
        return new ClienteResumoResponse(cliente.getId(), cliente.getNome(), cliente.getEmail());
    }
}

