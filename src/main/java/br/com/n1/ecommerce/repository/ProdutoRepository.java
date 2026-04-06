package br.com.n1.ecommerce.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.n1.ecommerce.model.Produto;
import jakarta.persistence.LockModeType;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Produto p
        where p.id in :ids
        order by p.id
        """)
    List<Produto> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
