package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ConsultaVeiculoServiceImpl implements ConsultaVeiculoService {
    @Value("${veiculos.service.url}")
    private String veiculosServiceUrl;

    private final RestTemplate restTemplate;
    private final VendaRepository vendaRepository;

    public ConsultaVeiculoServiceImpl(RestTemplate restTemplate, VendaRepository vendaRepository) {
        this.restTemplate = restTemplate;
        this.vendaRepository = vendaRepository;
    }


    @Override
    public Page<VeiculoDto> listarVeiculosPorStatus(VeiculoStatus status, Pageable pageable) {
        String url = UriComponentsBuilder.fromUriString(veiculosServiceUrl + "/veiculos")
                .queryParam("veiculoStatus", status.name())
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .queryParam("sort", "preco,asc")
                .toUriString();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );

            List<VeiculoDto> content = (List<VeiculoDto>) response.getBody().get("content");
            int totalElements = (int) response.getBody().get("totalElements");
            int totalPages = (int) response.getBody().get("totalPages");

            return new PageImpl<>(content, pageable, totalElements);

        } catch (Exception e) {
            System.err.println("Erro ao listar veículos do Serviço de Veículos: " + e.getMessage());
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
}