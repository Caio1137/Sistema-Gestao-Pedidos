package br.com.n1.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.n1.ecommerce.dto.PedidoCreateRequest;
import br.com.n1.ecommerce.dto.PedidoItemRequest;
import br.com.n1.ecommerce.dto.PedidoResponse;
import br.com.n1.ecommerce.dto.PedidoStatusUpdateRequest;
import br.com.n1.ecommerce.exception.BusinessException;
import br.com.n1.ecommerce.model.Cliente;
import br.com.n1.ecommerce.model.Endereco;
import br.com.n1.ecommerce.model.Produto;
import br.com.n1.ecommerce.model.StatusPedido;
import br.com.n1.ecommerce.repository.ClienteRepository;
import br.com.n1.ecommerce.repository.ProdutoRepository;

@SpringBootTest
@Transactional
class PedidoServiceIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void deveCriarPedidoCalcularTotalEReduzirEstoque() {
        Cliente cliente = salvarClienteComEndereco();
        Endereco endereco = cliente.getEnderecos().get(0);
        Produto notebook = produtoRepository.saveAndFlush(
            new Produto("Notebook", new BigDecimal("3000.00"), 10)
        );
        Produto mouse = produtoRepository.saveAndFlush(
            new Produto("Mouse", new BigDecimal("50.00"), 20)
        );

        PedidoResponse pedido = pedidoService.criar(new PedidoCreateRequest(
            cliente.getId(),
            endereco.getId(),
            List.of(
                new PedidoItemRequest(notebook.getId(), 1),
                new PedidoItemRequest(mouse.getId(), 2)
            )
        ));

        Produto notebookAtualizado = produtoRepository.findById(notebook.getId()).orElseThrow();
        Produto mouseAtualizado = produtoRepository.findById(mouse.getId()).orElseThrow();

        assertThat(pedido.status()).isEqualTo(StatusPedido.CRIADO);
        assertThat(pedido.total()).isEqualByComparingTo("3100.00");
        assertThat(pedido.itens()).hasSize(2);
        assertThat(notebookAtualizado.getEstoque()).isEqualTo(9);
        assertThat(mouseAtualizado.getEstoque()).isEqualTo(18);
    }

    @Test
    void naoDevePermitirPularEtapasNoFluxoDeStatus() {
        Cliente cliente = salvarClienteComEndereco();
        Endereco endereco = cliente.getEnderecos().get(0);
        Produto produto = produtoRepository.saveAndFlush(
            new Produto("Teclado", new BigDecimal("200.00"), 5)
        );

        PedidoResponse pedido = pedidoService.criar(new PedidoCreateRequest(
            cliente.getId(),
            endereco.getId(),
            List.of(new PedidoItemRequest(produto.getId(), 1))
        ));

        assertThatThrownBy(() -> pedidoService.atualizarStatus(
            pedido.id(),
            new PedidoStatusUpdateRequest(StatusPedido.ENVIADO)
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("fluxo permitido");
    }

    @Test
    void deveCancelarPedidoCriadoERestaurarEstoque() {
        Cliente cliente = salvarClienteComEndereco();
        Endereco endereco = cliente.getEnderecos().get(0);
        Produto produto = produtoRepository.saveAndFlush(
            new Produto("Monitor", new BigDecimal("900.00"), 4)
        );

        PedidoResponse pedido = pedidoService.criar(new PedidoCreateRequest(
            cliente.getId(),
            endereco.getId(),
            List.of(new PedidoItemRequest(produto.getId(), 2))
        ));

        pedidoService.cancelar(pedido.id());

        PedidoResponse pedidoCancelado = pedidoService.buscarPorId(pedido.id());
        Produto produtoAtualizado = produtoRepository.findById(produto.getId()).orElseThrow();

        assertThat(pedidoCancelado.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(produtoAtualizado.getEstoque()).isEqualTo(4);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveManterConsistenciaDeEstoqueEmPedidosConcorrentes() throws Exception {
        Cliente cliente = salvarClienteComEnderecos(10);
        Produto produto = produtoRepository.saveAndFlush(
            new Produto("Console", new BigDecimal("2500.00"), 3)
        );
        List<Long> enderecosIds = cliente.getEnderecos().stream()
            .map(Endereco::getId)
            .toList();
        int totalPedidos = enderecosIds.size();
        ExecutorService executor = Executors.newFixedThreadPool(totalPedidos);
        CountDownLatch ready = new CountDownLatch(totalPedidos);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> futures = IntStream.range(0, totalPedidos)
                .mapToObj(indice -> executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    try {
                        pedidoService.criar(new PedidoCreateRequest(
                            cliente.getId(),
                            enderecosIds.get(indice),
                            List.of(new PedidoItemRequest(produto.getId(), 1))
                        ));
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                }))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int pedidosCriadosComSucesso = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(15, TimeUnit.SECONDS)) {
                    pedidosCriadosComSucesso++;
                }
            }

            Produto produtoAtualizado = produtoRepository.findById(produto.getId()).orElseThrow();
            long pedidosCriados = pedidoService.listar().stream()
                .filter(pedido -> pedido.cliente().id().equals(cliente.getId()))
                .count();

            assertThat(pedidosCriadosComSucesso).isEqualTo(3);
            assertThat(pedidosCriados).isEqualTo(3);
            assertThat(produtoAtualizado.getEstoque()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private Cliente salvarClienteComEndereco() {
        return salvarClienteComEnderecos(1);
    }

    private Cliente salvarClienteComEnderecos(int quantidade) {
        Cliente cliente = new Cliente("Joao", "joao+" + UUID.randomUUID() + "@email.com");
        for (int indice = 1; indice <= quantidade; indice++) {
            cliente.adicionarEndereco(new Endereco(
                "Rua " + indice,
                "Sao Paulo",
                String.format("01000-%03d", indice)
            ));
        }
        return clienteRepository.saveAndFlush(cliente);
    }
}
