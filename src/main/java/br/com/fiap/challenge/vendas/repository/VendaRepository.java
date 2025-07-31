package br.com.fiap.challenge.vendas.repository;

import br.com.fiap.challenge.vendas.model.VendaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendaRepository extends JpaRepository<VendaModel, UUID> {
}