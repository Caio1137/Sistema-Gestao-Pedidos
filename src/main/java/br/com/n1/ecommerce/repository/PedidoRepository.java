package br.com.n1.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.n1.ecommerce.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    boolean existsByCliente_Id(Long clienteId);

    boolean existsByEnderecoEntrega_Id(Long enderecoId);

    @Query("""
        select distinct p
        from Pedido p
        left join fetch p.cliente
        left join fetch p.enderecoEntrega
        left join fetch p.itens i
        left join fetch i.produto
        order by p.data desc
        """)
    List<Pedido> findAllWithDetails();

    @Query("""
        select distinct p
        from Pedido p
        left join fetch p.cliente
        left join fetch p.enderecoEntrega
        left join fetch p.itens i
        left join fetch i.produto
        where p.id = :id
        """)
    Optional<Pedido> findByIdWithDetails(@Param("id") Long id);
}

