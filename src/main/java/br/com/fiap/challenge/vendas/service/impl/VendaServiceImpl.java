package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import br.com.fiap.challenge.vendas.service.VendaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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
    public void cancelVenda(VendaModel vendaModel) {
        vendaRepository.save(vendaModel);
    }

    @Override
    public Optional<VendaModel> findByVendaId(UUID vendaId) {
        return vendaRepository.findByVendaId(vendaId);
    }

    @Override
    public List<VendaModel> listAllVendas() {
        return vendaRepository.findAll();
    }
}
