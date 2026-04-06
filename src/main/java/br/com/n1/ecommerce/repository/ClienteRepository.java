package br.com.n1.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.n1.ecommerce.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
        select distinct c
        from Cliente c
        left join fetch c.enderecos
        order by c.nome
        """)
    List<Cliente> findAllWithEnderecos();

    @Query("""
        select distinct c
        from Cliente c
        left join fetch c.enderecos
        where c.id = :id
        """)
    Optional<Cliente> findByIdWithEnderecos(@Param("id") Long id);
}

