package br.com.n1.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.n1.ecommerce.model.ItemPedido;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    boolean existsByProduto_Id(Long produtoId);
}

