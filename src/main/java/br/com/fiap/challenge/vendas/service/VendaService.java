package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.model.VendaModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaService {
    VendaModel saveVenda(VendaModel vendaModel);
    Optional<VendaModel> findByVendaId(UUID vendaId);
    List<VendaModel> listAllVendas();
    void notificationProcessPayment(String codigoPagamento, String statusPagamento);
}
