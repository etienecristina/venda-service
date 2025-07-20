package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import br.com.fiap.challenge.vendas.service.VendaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VendaServiceImpl implements VendaService {

    private final RestTemplate restTemplate;

    private final VendaRepository vendaRepository;

    @Value("${veiculos.service.url}")
    private String veiculosServiceUrl;

    public VendaServiceImpl(RestTemplate restTemplate, VendaRepository vendaRepository) {
        this.restTemplate = restTemplate;
        this.vendaRepository = vendaRepository;
    }

    @Override
    public VendaModel saveVenda(VendaModel vendaModel) {
        return vendaRepository.save(vendaModel);
    }

    @Override
    public Optional<VendaModel> findByVendaId(UUID vendaId) {
        return vendaRepository.findByVendaId(vendaId);
    }

    @Override
    public List<VendaModel> listAllVendas() {
        return vendaRepository.findAll();
    }

    @Override
    public void notificationProcessPayment(String codigoPagamento, String statusPagamento) {
        Optional<VendaModel> vendaOptional = vendaRepository.findByCodigoPagamento(codigoPagamento);

        if (vendaOptional.isEmpty()) {
            throw new RuntimeException("Venda não encontrada para o código de pagamento: " + codigoPagamento);
        }

        VendaModel venda = vendaOptional.get();

        if ("EFETUADO".equalsIgnoreCase(statusPagamento)) {
            venda.setPagamentoStatus(PagamentoStatus.EFETUADO);
            try {
                String veiculoUpdateUrl = veiculosServiceUrl + "/veiculos/" + venda.getVeiculoId() + "/vender";
                restTemplate.patchForObject(veiculoUpdateUrl, null, Void.class);

            } catch (Exception e) {
                System.err.println("Erro ao chamar serviço de veículos para vender: " + venda.getVeiculoId() + " - " + e.getMessage());
            }
        } else if ("CANCELADO".equalsIgnoreCase(statusPagamento)) {
            venda.setPagamentoStatus(PagamentoStatus.CANCELADO);
        } else {
            throw new IllegalArgumentException("Status de pagamento desconhecido: " + statusPagamento);
        }

        vendaRepository.save(venda);
    }
}