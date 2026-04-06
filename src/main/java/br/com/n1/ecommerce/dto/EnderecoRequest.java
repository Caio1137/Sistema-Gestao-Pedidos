package br.com.n1.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
    @Positive(message = "Id do endereco deve ser um identificador valido")
    Long id,

    @NotBlank(message = "Rua e obrigatoria")
    @Size(max = 150, message = "Rua deve ter no maximo 150 caracteres")
    String rua,

    @NotBlank(message = "Cidade e obrigatoria")
    @Size(max = 100, message = "Cidade deve ter no maximo 100 caracteres")
    String cidade,

    @NotBlank(message = "CEP e obrigatorio")
    @Size(max = 20, message = "CEP deve ter no maximo 20 caracteres")
    String cep
) {
}
