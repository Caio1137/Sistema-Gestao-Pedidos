package br.com.n1.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.n1.ecommerce.dto.ClienteRequest;
import br.com.n1.ecommerce.dto.ClienteResponse;
import br.com.n1.ecommerce.dto.EnderecoRequest;
import br.com.n1.ecommerce.dto.PedidoCreateRequest;
import br.com.n1.ecommerce.dto.PedidoItemRequest;
import br.com.n1.ecommerce.exception.BusinessException;
import br.com.n1.ecommerce.model.Cliente;
import br.com.n1.ecommerce.model.Endereco;
import br.com.n1.ecommerce.model.Produto;
import br.com.n1.ecommerce.repository.ClienteRepository;
import br.com.n1.ecommerce.repository.ProdutoRepository;

@SpringBootTest
@Transactional
class ClienteServiceIntegrationTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void naoDevePermitirCadastroDeClientesComEmailsDuplicados() {
        ClienteRequest primeiroCliente = new ClienteRequest(
            "Maria",
            "maria@email.com",
            List.of()
        );

        ClienteRequest segundoCliente = new ClienteRequest(
            "Outra Maria",
            "MARIA@email.com",
            List.of()
        );

        clienteService.criar(primeiroCliente);

        assertThatThrownBy(() -> clienteService.criar(segundoCliente))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("email");
    }

    @Test
    void devePermitirAtualizarEnderecosExistentesDeClienteComPedido() {
        Cliente cliente = salvarClienteComDoisEnderecos();
        Endereco enderecoDoPedido = cliente.getEnderecos().get(0);
        Endereco enderecoLivre = cliente.getEnderecos().get(1);
        Produto produto = produtoRepository.saveAndFlush(
            new Produto("Notebook", new BigDecimal("3500.00"), 5)
        );

        pedidoService.criar(new PedidoCreateRequest(
            cliente.getId(),
            enderecoDoPedido.getId(),
            List.of(new PedidoItemRequest(produto.getId(), 1))
        ));

        ClienteResponse atualizado = clienteService.atualizar(cliente.getId(), new ClienteRequest(
            "Joao Atualizado",
            "JOAO.NOVO@email.com",
            List.of(
                new EnderecoRequest(enderecoDoPedido.getId(), "Rua A, 123", "Sao Paulo", "01000-123"),
                new EnderecoRequest(enderecoLivre.getId(), "Rua B, 456", "Campinas", "13000-456"),
                new EnderecoRequest(null, "Rua Nova", "Santos", "11000-000")
            )
        ));

        assertThat(atualizado.nome()).isEqualTo("Joao Atualizado");
        assertThat(atualizado.email()).isEqualTo("joao.novo@email.com");
        assertThat(atualizado.enderecos()).hasSize(3);
        assertThat(atualizado.enderecos())
            .anySatisfy(endereco -> {
                assertThat(endereco.id()).isEqualTo(enderecoDoPedido.getId());
                assertThat(endereco.rua()).isEqualTo("Rua A, 123");
            })
            .anySatisfy(endereco -> {
                assertThat(endereco.id()).isEqualTo(enderecoLivre.getId());
                assertThat(endereco.rua()).isEqualTo("Rua B, 456");
            })
            .anySatisfy(endereco -> {
                assertThat(endereco.id()).isNotNull();
                assertThat(endereco.rua()).isEqualTo("Rua Nova");
            });
    }

    @Test
    void naoDevePermitirRemoverEnderecoVinculadoAPedido() {
        Cliente cliente = salvarClienteComDoisEnderecos();
        Endereco enderecoDoPedido = cliente.getEnderecos().get(0);
        Endereco enderecoLivre = cliente.getEnderecos().get(1);
        Produto produto = produtoRepository.saveAndFlush(
            new Produto("Monitor", new BigDecimal("1500.00"), 3)
        );

        pedidoService.criar(new PedidoCreateRequest(
            cliente.getId(),
            enderecoDoPedido.getId(),
            List.of(new PedidoItemRequest(produto.getId(), 1))
        ));

        assertThatThrownBy(() -> clienteService.atualizar(cliente.getId(), new ClienteRequest(
            "Joao",
            "joao.remocao@email.com",
            List.of(new EnderecoRequest(enderecoLivre.getId(), "Rua B", "Campinas", "13000-000"))
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("remover endereco vinculado");
    }

    private Cliente salvarClienteComDoisEnderecos() {
        Cliente cliente = new Cliente("Joao", "joao+" + UUID.randomUUID() + "@email.com");
        cliente.adicionarEndereco(new Endereco("Rua A", "Sao Paulo", "01000-000"));
        cliente.adicionarEndereco(new Endereco("Rua B", "Campinas", "13000-000"));
        return clienteRepository.saveAndFlush(cliente);
    }
}
