package br.com.n1.ecommerce.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.n1.ecommerce.dto.ClienteRequest;
import br.com.n1.ecommerce.dto.ClienteResponse;
import br.com.n1.ecommerce.dto.EnderecoRequest;
import br.com.n1.ecommerce.exception.BusinessException;
import br.com.n1.ecommerce.exception.ResourceNotFoundException;
import br.com.n1.ecommerce.model.Cliente;
import br.com.n1.ecommerce.model.Endereco;
import br.com.n1.ecommerce.repository.ClienteRepository;
import br.com.n1.ecommerce.repository.PedidoRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailUnico(email, null);

        Cliente cliente = new Cliente(request.nome().trim(), email);
        converterEnderecos(request.enderecos()).forEach(cliente::adicionarEndereco);

        return ClienteResponse.fromEntity(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllWithEnderecos().stream()
            .map(ClienteResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.fromEntity(buscarClienteComEnderecos(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarClienteComEnderecos(id);
        String email = normalizarEmail(request.email());

        validarEmailUnico(email, id);

        cliente.setNome(request.nome().trim());
        cliente.setEmail(email);

        if (request.enderecos() != null) {
            sincronizarEnderecos(cliente, request.enderecos());
        }

        return ClienteResponse.fromEntity(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));

        if (pedidoRepository.existsByCliente_Id(id)) {
            throw new BusinessException(
                "Nao e possivel excluir cliente com pedidos cadastrados.",
                HttpStatus.BAD_REQUEST
            );
        }

        clienteRepository.delete(cliente);
    }

    private Cliente buscarClienteComEnderecos(Long id) {
        return clienteRepository.findByIdWithEnderecos(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));
    }

    private void validarEmailUnico(String email, Long id) {
        boolean emailDuplicado = id == null
            ? clienteRepository.existsByEmailIgnoreCase(email)
            : clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id);

        if (emailDuplicado) {
            throw new BusinessException("Ja existe um cliente com o email informado.", HttpStatus.CONFLICT);
        }
    }

    private List<Endereco> converterEnderecos(List<EnderecoRequest> enderecos) {
        if (enderecos == null) {
            return List.of();
        }

        return enderecos.stream()
            .map(this::criarEndereco)
            .toList();
    }

    private void sincronizarEnderecos(Cliente cliente, List<EnderecoRequest> enderecosRequest) {
        Map<Long, Endereco> enderecosExistentes = cliente.getEnderecos().stream()
            .collect(Collectors.toMap(Endereco::getId, Function.identity()));
        Set<Long> idsMantidos = new java.util.LinkedHashSet<>();
        List<Endereco> novosEnderecos = new ArrayList<>();

        for (EnderecoRequest enderecoRequest : enderecosRequest) {
            if (enderecoRequest.id() == null) {
                novosEnderecos.add(criarEndereco(enderecoRequest));
                continue;
            }

            Endereco enderecoExistente = enderecosExistentes.get(enderecoRequest.id());
            if (enderecoExistente == null) {
                throw new BusinessException(
                    "O endereco informado nao pertence ao cliente.",
                    HttpStatus.BAD_REQUEST
                );
            }

            if (!idsMantidos.add(enderecoRequest.id())) {
                throw new BusinessException(
                    "Endereco informado mais de uma vez na requisicao.",
                    HttpStatus.BAD_REQUEST
                );
            }

            enderecoExistente.setRua(enderecoRequest.rua().trim());
            enderecoExistente.setCidade(enderecoRequest.cidade().trim());
            enderecoExistente.setCep(enderecoRequest.cep().trim());
        }

        List<Endereco> enderecosParaRemover = cliente.getEnderecos().stream()
            .filter(endereco -> !idsMantidos.contains(endereco.getId()))
            .toList();

        for (Endereco endereco : enderecosParaRemover) {
            if (pedidoRepository.existsByEnderecoEntrega_Id(endereco.getId())) {
                throw new BusinessException(
                    "Nao e possivel remover endereco vinculado a pedido.",
                    HttpStatus.BAD_REQUEST
                );
            }

            cliente.removerEndereco(endereco);
        }

        novosEnderecos.forEach(cliente::adicionarEndereco);
    }

    private Endereco criarEndereco(EnderecoRequest endereco) {
        return new Endereco(
            endereco.rua().trim(),
            endereco.cidade().trim(),
            endereco.cep().trim()
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
