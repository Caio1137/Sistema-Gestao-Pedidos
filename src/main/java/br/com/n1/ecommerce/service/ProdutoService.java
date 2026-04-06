package br.com.n1.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.n1.ecommerce.dto.ProdutoRequest;
import br.com.n1.ecommerce.dto.ProdutoResponse;
import br.com.n1.ecommerce.exception.BusinessException;
import br.com.n1.ecommerce.exception.ResourceNotFoundException;
import br.com.n1.ecommerce.model.Produto;
import br.com.n1.ecommerce.repository.ItemPedidoRepository;
import br.com.n1.ecommerce.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public ProdutoService(ProdutoRepository produtoRepository, ItemPedidoRepository itemPedidoRepository) {
        this.produtoRepository = produtoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        validarProduto(request.preco(), request.estoque());
        Produto produto = new Produto(request.nome().trim(), request.preco(), request.estoque());
        return ProdutoResponse.fromEntity(produtoRepository.save(produto));
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtoRepository.findAll().stream()
            .map(ProdutoResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return ProdutoResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        validarProduto(request.preco(), request.estoque());

        Produto produto = buscarEntidade(id);
        produto.setNome(request.nome().trim());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque());

        return ProdutoResponse.fromEntity(produto);
    }

    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarEntidade(id);

        if (itemPedidoRepository.existsByProduto_Id(id)) {
            throw new BusinessException(
                "Nao e possivel excluir produto vinculado a itens de pedido.",
                HttpStatus.BAD_REQUEST
            );
        }

        produtoRepository.delete(produto);
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado."));
    }

    private void validarProduto(BigDecimal preco, Integer estoque) {
        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Preco deve ser maior que zero.", HttpStatus.BAD_REQUEST);
        }

        if (estoque < 0) {
            throw new BusinessException("Estoque nao pode ser negativo.", HttpStatus.BAD_REQUEST);
        }
    }
}

