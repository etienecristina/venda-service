package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@Slf4j
public class ConsultaVeiculoServiceImpl implements ConsultaVeiculoService {
    @Value("${veiculos.service.url}")
    private String veiculosServiceUrl;

    @Value("${AUTH_TOKEN}")
    private String veiculosAuthToken;

    private final RestTemplate restTemplate;

    public ConsultaVeiculoServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Page<VeiculoDto> listarVeiculosPorStatus(VeiculoStatus status, Pageable pageable) {
        String url = UriComponentsBuilder.fromUriString(veiculosServiceUrl)
                .queryParam("veiculoStatus", status.name())
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .queryParam("sort", "preco,asc")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(veiculosAuthToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
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

    @Override
    public Optional<VeiculoDto> findByVeiculoId(UUID veiculoId) {
        String url = veiculosServiceUrl + "/" + veiculoId.toString();
        log.info("Buscando veículo na URL: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + veiculosAuthToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<VeiculoDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VeiculoDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Veículo encontrado: {}", response.getBody().getVeiculoId());
                return Optional.of(response.getBody());
            } else {
                log.warn("Veículo com ID {} não encontrado ou resposta vazia. Status: {}", veiculoId, response.getStatusCode());
                return Optional.empty();
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Veículo com ID {} não encontrado no serviço de veículos (404 Not Found).", veiculoId);
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.error("Erro do cliente ao buscar veículo {}. Status: {}, Corpo: {}",
                    veiculoId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            log.error("Erro do servidor ao buscar veículo {}. Status: {}, Corpo: {}",
                    veiculoId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar veículo {}: {}", veiculoId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean atualizarStatusVeiculo(UUID veiculoId) {
        String url = veiculosServiceUrl + "/" + veiculoId + "/vender";
        log.info("Enviando requisição para vender o veículo {} na URL: {}", veiculoId, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(veiculosAuthToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Veículo {} marcado como vendido com sucesso! Resposta: {}", veiculoId, response.getBody());
                return true;
            } else {
                log.error("❌ Falha ao marcar veículo {} como vendido. Status: {}, Resposta: {}",
                        veiculoId, response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Erro HTTP ao marcar veículo {} como vendido: {} - {}",
                    veiculoId, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao marcar veículo {} como vendido: {}", veiculoId, e.getMessage());
            return false;
        }
    }
}