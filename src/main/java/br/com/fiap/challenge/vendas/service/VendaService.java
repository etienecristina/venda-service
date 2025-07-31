package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.model.VendaModel;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaService {
    VendaModel saveVenda(VendaModel vendaModel);
    Optional<VendaModel> findByVendaId(UUID vendaId);
    List<VendaModel> listAllVendas();
}