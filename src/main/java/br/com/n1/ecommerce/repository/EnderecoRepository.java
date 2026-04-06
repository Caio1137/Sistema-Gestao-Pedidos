package br.com.n1.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.n1.ecommerce.model.Endereco;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    Optional<Endereco> findByIdAndCliente_Id(Long id, Long clienteId);
}

