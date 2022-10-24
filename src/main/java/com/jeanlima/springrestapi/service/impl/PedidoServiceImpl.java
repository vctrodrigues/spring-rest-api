package com.jeanlima.springrestapi.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.jeanlima.springrestapi.model.*;
import com.jeanlima.springrestapi.repository.*;
import org.springframework.stereotype.Service;

import com.jeanlima.springrestapi.enums.StatusPedido;
import com.jeanlima.springrestapi.exception.PedidoNaoEncontradoException;
import com.jeanlima.springrestapi.exception.RegraNegocioException;
import com.jeanlima.springrestapi.rest.dto.ItemPedidoDTO;
import com.jeanlima.springrestapi.rest.dto.PedidoDTO;
import com.jeanlima.springrestapi.service.PedidoService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.support.RestGatewaySupport;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository repository;
    private final ClienteRepository clientesRepository;
    private final ProdutoRepository produtosRepository;
    private final ItemPedidoRepository itemsPedidoRepository;
    private final EstoqueRepository estoqueRepository;

    @Override
    @Transactional
    public Pedido salvar(PedidoDTO dto) {
        Integer idCliente = dto.getCliente();
        Cliente cliente = clientesRepository
                .findById(idCliente)
                .orElseThrow(() -> new RegraNegocioException("Código de cliente inválido."));

        Pedido pedido = new Pedido();
        pedido.setDataPedido(LocalDate.now());
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.REALIZADO);

        List<ItemPedido> itemsPedido = converterItems(pedido, dto.getItems());

        Map<Integer, Integer> estoqueQuantidade = new HashMap<>();

        itemsPedido.forEach(item -> {
            Estoque estoque = estoqueRepository.findByNomeProduto(item.getProduto().getDescricao()).get(0);

            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new RegraNegocioException("O produto [" + item.getProduto().getDescricao() + "] só possui [" + estoque.getQuantidade() + "] unidades.");
            }

            estoqueQuantidade.put(estoque.getId(), estoque.getQuantidade() - item.getQuantidade());
        });

        estoqueQuantidade.forEach((id, quantidade) -> {
            estoqueRepository.findById(id).map(estoque -> {
                estoque.setQuantidade(quantidade);
                estoqueRepository.save(estoque);
                return estoque;
            });
        });

        final BigDecimal[] total = {BigDecimal.ZERO};
        itemsPedido.forEach(item -> {
            total[0] = total[0].add(item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
        });

        pedido.setTotal(total[0]);

        repository.save(pedido);
        itemsPedidoRepository.saveAll(itemsPedido);
        pedido.setItens(itemsPedido);
        return pedido;
    }

    @Override
    @Transactional
    public Pedido atualizar(PedidoDTO dto, Pedido referencia) {
        Pedido pedido = new Pedido();
        pedido.setDataPedido(referencia.getDataPedido());
        pedido.setCliente(referencia.getCliente());
        pedido.setStatus(referencia.getStatus());

        List<ItemPedido> itemsPedido = pedido.getItens();

        Map<Integer, Integer> estoqueQuantidade = new HashMap<>();

        itemsPedido.forEach(item -> {
            Integer diferenca = item.getQuantidade() - referencia.getItens().stream().filter(_item -> _item.getProduto().getId() == item.getProduto().getId()).findFirst().orElse(new ItemPedido()).getQuantidade();
            Estoque estoque = estoqueRepository.findByNomeProduto(item.getProduto().getDescricao()).get(0);

            if (estoque.getQuantidade() < diferenca) {
                throw new RegraNegocioException("O produto [" + item.getProduto().getDescricao() + "] só possui [" + estoque.getQuantidade() + "] unidades.");
            }

            estoqueQuantidade.put(estoque.getId(), diferenca);
        });

        estoqueQuantidade.forEach((id, quantidade) -> {
            estoqueRepository.findById(id).map(estoque -> {
                Integer novaQuantidade = quantidade;

                if (novaQuantidade < 0) {
                    novaQuantidade = estoque.getQuantidade() + (novaQuantidade * -1);
                }

                estoque.setQuantidade(novaQuantidade);
                estoqueRepository.save(estoque);
                return estoque;
            });
        });

        BigDecimal total = BigDecimal.ZERO;
        itemsPedido.forEach(item -> {
            total.add(item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
        });
        pedido.setTotal(total);

        repository.save(pedido);
        itemsPedidoRepository.saveAll(itemsPedido);
        pedido.setItens(itemsPedido);
        return pedido;
    }

    private List<ItemPedido> converterItems(Pedido pedido, List<ItemPedidoDTO> items) {
        if (items.isEmpty()) {
            throw new RegraNegocioException("Não é possível realizar um pedido sem items.");
        }

        return items
                .stream()
                .map(dto -> {
                    Integer idProduto = dto.getProduto();
                    Produto produto = produtosRepository
                            .findById(idProduto)
                            .orElseThrow(
                                    () -> new RegraNegocioException(
                                            "Código de produto inválido: " + idProduto
                                    ));

                    ItemPedido itemPedido = new ItemPedido();
                    itemPedido.setQuantidade(dto.getQuantidade());
                    itemPedido.setPedido(pedido);
                    itemPedido.setProduto(produto);
                    return itemPedido;
                }).collect(Collectors.toList());

    }

    @Override
    public Optional<Pedido> obterPedidoCompleto(Integer id) {

        return repository.findByIdFetchItens(id);
    }

    @Override
    public void atualizaStatus(Integer id, StatusPedido statusPedido) {
        repository
                .findById(id)
                .map(pedido -> {
                    pedido.setStatus(statusPedido);
                    return repository.save(pedido);
                }).orElseThrow(() -> new PedidoNaoEncontradoException());

    }

    @Override
    public void delete(Integer id) {
        repository.findById(id).map(pedido -> {
            repository.delete(pedido);
            return null;
        }).orElseThrow(() -> new PedidoNaoEncontradoException());
    }
}
