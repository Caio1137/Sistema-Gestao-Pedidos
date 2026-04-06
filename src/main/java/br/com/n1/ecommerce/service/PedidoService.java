package br.com.n1.ecommerce.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.n1.ecommerce.dto.PedidoCreateRequest;
import br.com.n1.ecommerce.dto.PedidoItemRequest;
import br.com.n1.ecommerce.dto.PedidoResponse;
import br.com.n1.ecommerce.dto.PedidoStatusUpdateRequest;
import br.com.n1.ecommerce.exception.BusinessException;
import br.com.n1.ecommerce.exception.ResourceNotFoundException;
import br.com.n1.ecommerce.model.Cliente;
import br.com.n1.ecommerce.model.Endereco;
import br.com.n1.ecommerce.model.ItemPedido;
import br.com.n1.ecommerce.model.Pedido;
import br.com.n1.ecommerce.model.Produto;
import br.com.n1.ecommerce.model.StatusPedido;
import br.com.n1.ecommerce.repository.ClienteRepository;
import br.com.n1.ecommerce.repository.EnderecoRepository;
import br.com.n1.ecommerce.repository.PedidoRepository;
import br.com.n1.ecommerce.repository.ProdutoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(
        PedidoRepository pedidoRepository,
        ClienteRepository clienteRepository,
        EnderecoRepository enderecoRepository,
        ProdutoRepository produtoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.enderecoRepository = enderecoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public PedidoResponse criar(PedidoCreateRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Endereco endereco = buscarEnderecoDoCliente(request.enderecoId(), cliente.getId());

        if (pedidoRepository.existsByEnderecoEntrega_Id(endereco.getId())) {
            throw new BusinessException(
                "O endereco informado ja esta vinculado a outro pedido.",
                HttpStatus.BAD_REQUEST
            );
        }

        Map<Long, Integer> itensConsolidados = consolidarItens(request.itens());
        Map<Long, Produto> produtos = buscarProdutos(itensConsolidados);

        validarEstoque(produtos, itensConsolidados);

        Pedido pedido = new Pedido(cliente, endereco);

        itensConsolidados.forEach((produtoId, quantidade) -> {
            Produto produto = produtos.get(produtoId);
            produto.reduzirEstoque(quantidade);
            pedido.adicionarItem(new ItemPedido(produto, quantidade, produto.getPreco()));
        });

        Pedido salvo = pedidoRepository.saveAndFlush(pedido);
        return PedidoResponse.fromEntity(buscarPedidoComDetalhes(salvo.getId()));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAllWithDetails().stream()
            .map(PedidoResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return PedidoResponse.fromEntity(buscarPedidoComDetalhes(id));
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, PedidoStatusUpdateRequest request) {
        Pedido pedido = buscarPedidoComDetalhes(id);
        StatusPedido novoStatus = request.status();

        if (novoStatus == StatusPedido.CANCELADO) {
            throw new BusinessException(
                "Use o endpoint de cancelamento para cancelar um pedido.",
                HttpStatus.BAD_REQUEST
            );
        }

        validarFluxoStatus(pedido.getStatus(), novoStatus);
        pedido.setStatus(novoStatus);

        return PedidoResponse.fromEntity(pedido);
    }

    @Transactional
    public void cancelar(Long id) {
        Pedido pedido = buscarPedidoComDetalhes(id);

        if (pedido.getStatus() != StatusPedido.CRIADO) {
            throw new BusinessException(
                "Apenas pedidos com status CRIADO podem ser cancelados.",
                HttpStatus.BAD_REQUEST
            );
        }

        for (ItemPedido item : pedido.getItens()) {
            item.getProduto().aumentarEstoque(item.getQuantidade());
        }

        pedido.setStatus(StatusPedido.CANCELADO);
    }

    private Map<Long, Integer> consolidarItens(List<PedidoItemRequest> itens) {
        Map<Long, Integer> itensConsolidados = new LinkedHashMap<>();

        for (PedidoItemRequest item : itens) {
            itensConsolidados.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        return itensConsolidados;
    }

    private Map<Long, Produto> buscarProdutos(Map<Long, Integer> itensConsolidados) {
        List<Long> produtoIds = itensConsolidados.keySet().stream()
            .sorted()
            .toList();

        Map<Long, Produto> produtos = produtoRepository.findAllByIdInForUpdate(produtoIds).stream()
            .collect(Collectors.toMap(
                Produto::getId,
                Function.identity(),
                (primeiro, segundo) -> primeiro,
                LinkedHashMap::new
            ));

        if (produtos.size() != produtoIds.size()) {
            Long produtoAusente = produtoIds.stream()
                .filter(produtoId -> !produtos.containsKey(produtoId))
                .findFirst()
                .orElseThrow();

            throw new ResourceNotFoundException("Produto " + produtoAusente + " nao encontrado.");
        }

        return produtos;
    }

    private void validarEstoque(Map<Long, Produto> produtos, Map<Long, Integer> itensConsolidados) {
        for (Map.Entry<Long, Integer> entry : itensConsolidados.entrySet()) {
            Produto produto = produtos.get(entry.getKey());
            Integer quantidadeSolicitada = entry.getValue();

            if (produto.getEstoque() < quantidadeSolicitada) {
                throw new BusinessException(
                    "Estoque insuficiente para o produto " + produto.getNome() + ".",
                    HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private void validarFluxoStatus(StatusPedido statusAtual, StatusPedido novoStatus) {
        if (statusAtual == novoStatus) {
            throw new BusinessException("Pedido ja esta no status informado.", HttpStatus.BAD_REQUEST);
        }

        switch (statusAtual) {
            case CRIADO -> {
                if (novoStatus != StatusPedido.PAGO) {
                    throw new BusinessException(
                        "O fluxo permitido e CRIADO -> PAGO -> ENVIADO.",
                        HttpStatus.BAD_REQUEST
                    );
                }
            }
            case PAGO -> {
                if (novoStatus != StatusPedido.ENVIADO) {
                    throw new BusinessException(
                        "O fluxo permitido e CRIADO -> PAGO -> ENVIADO.",
                        HttpStatus.BAD_REQUEST
                    );
                }
            }
            case ENVIADO -> throw new BusinessException(
                "Pedido enviado nao pode voltar ou avancar de status.",
                HttpStatus.BAD_REQUEST
            );
            case CANCELADO -> throw new BusinessException(
                "Pedido cancelado nao pode mudar de status.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private Pedido buscarPedidoComDetalhes(Long id) {
        return pedidoRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado."));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));
    }

    private Endereco buscarEnderecoDoCliente(Long enderecoId, Long clienteId) {
        return enderecoRepository.findByIdAndCliente_Id(enderecoId, clienteId)
            .orElseThrow(() -> new BusinessException(
                "O endereco informado nao pertence ao cliente.",
                HttpStatus.BAD_REQUEST
            ));
    }
}
