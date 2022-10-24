package com.jeanlima.springrestapi.repository;

import com.jeanlima.springrestapi.model.Estoque;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping("/api/estoque")
@RestController
public class EstoqueController {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @GetMapping("{id}")
    public Estoque getEstoqueById(@PathVariable Integer id) {
        return estoqueRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estoque não encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Estoque save(@RequestBody Estoque estoque) { return estoqueRepository.save(estoque); }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        estoqueRepository.findById(id).map(estoque -> {
            estoqueRepository.delete(estoque);
            return estoque;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estoque não encontrado"));
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Integer id, @RequestBody Estoque estoque) {
        estoqueRepository.findById(id).map(_estoque -> {
            estoque.setId((_estoque.getId()));
            estoqueRepository.save(estoque);
            return estoque;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estoque não encontrado"));
    }

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateQuantidade(@PathVariable Integer id, @RequestBody Estoque estoque) {
        estoqueRepository.findById(id).map(_estoque -> {
            _estoque.setQuantidade(estoque.getQuantidade());

            estoqueRepository.save(_estoque);
            return _estoque;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estoque não encontrado"));
    }


    @GetMapping
    public List<Estoque> find(Estoque filtro) {
        ExampleMatcher matcher = ExampleMatcher
                .matching()
                .withIgnoreCase()
                .withStringMatcher(
                        ExampleMatcher.StringMatcher.CONTAINING);

        Example example = Example.of(filtro, matcher);
        return estoqueRepository.findAll(example);
    }

    @GetMapping("/produto/{produto}")
    public List<Estoque> findByNomeProduto(@PathVariable String produto) {
        return estoqueRepository.findByNomeProduto(produto);
    }
}
