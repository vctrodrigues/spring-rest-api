package com.jeanlima.springrestapi.repository;

import com.jeanlima.springrestapi.model.Cliente;
import com.jeanlima.springrestapi.model.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EstoqueRepository extends JpaRepository<Estoque,Integer> {

    @Query(value = "SELECT * FROM estoque e INNER JOIN produto p ON e.produto_id = p.id WHERE p.descricao LIKE %:produto%" +
            "", nativeQuery = true)
    List<Estoque> findByNomeProduto(@Param("produto") String produto);

}
