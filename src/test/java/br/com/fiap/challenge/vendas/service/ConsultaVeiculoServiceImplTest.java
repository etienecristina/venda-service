package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.service.impl.ConsultaVeiculoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ConsultaVeiculoServiceImplTest {

    @InjectMocks
    private ConsultaVeiculoServiceImpl consultaVeiculoService;

    private RestTemplate restTemplate = new RestTemplate();
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        ReflectionTestUtils.setField(consultaVeiculoService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(consultaVeiculoService, "veiculosServiceUrl", "http://localhost:8080/veiculos");
        ReflectionTestUtils.setField(consultaVeiculoService, "veiculosAuthToken", "token-de-teste");
    }

    @Test
    @DisplayName("Deve buscar um veículo com sucesso por ID")
    void deveBuscarVeiculoPorIdComSucesso() {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        String url = "http://localhost:8080/veiculos/" + veiculoId;
        String veiculoJson = "{\"veiculoId\":\"" + veiculoId + "\", \"marca\":\"Ford\", \"modelo\":\"Mustang\", \"veiculoStatus\":\"DISPONIVEL\"}";

        // WHEN
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andRespond(withSuccess(veiculoJson, MediaType.APPLICATION_JSON));

        Optional<VeiculoDto> resultado = consultaVeiculoService.findByVeiculoId(veiculoId);

        // THEN
        mockServer.verify();
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getVeiculoId()).isEqualTo(veiculoId);
        assertThat(resultado.get().getMarca()).isEqualTo("Ford");
    }

    @Test
    @DisplayName("Não deve encontrar um veículo e retornar Optional vazio")
    void naoDeveEncontrarVeiculo() {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        String url = "http://localhost:8080/veiculos/" + veiculoId;

        // WHEN
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<VeiculoDto> resultado = consultaVeiculoService.findByVeiculoId(veiculoId);

        // THEN
        mockServer.verify();
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar o status do veículo com sucesso")
    void deveAtualizarStatusVeiculoComSucesso() {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        String url = "http://localhost:8080/veiculos/" + veiculoId + "/vender";

        // WHEN
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andRespond(withSuccess());

        boolean resultado = consultaVeiculoService.atualizarStatusVeiculo(veiculoId);

        // THEN
        mockServer.verify();
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Não deve atualizar o status do veículo e retornar false em caso de erro")
    void naoDeveAtualizarStatusVeiculoEmCasoDeErro() {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        String url = "http://localhost:8080/veiculos/" + veiculoId + "/vender";

        // WHEN
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        boolean resultado = consultaVeiculoService.atualizarStatusVeiculo(veiculoId);

        // THEN
        mockServer.verify();
        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve listar veículos por status com sucesso")
    void deveListarVeiculosPorStatusComSucesso() {
        // GIVEN
        PageRequest pageable = PageRequest.of(0, 10);
        String url = "http://localhost:8080/veiculos?veiculoStatus=DISPONIVEL&page=0&size=10&sort=preco,asc";
        String responseJson = "{\"content\":[{\"veiculoId\":\"" + UUID.randomUUID() + "\", \"marca\":\"Ford\"}], \"totalElements\":1, \"totalPages\":1}";

        // WHEN
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Page<VeiculoDto> resultado = consultaVeiculoService.listarVeiculosPorStatus(VeiculoStatus.DISPONIVEL, pageable);

        // THEN
        mockServer.verify();
        assertThat(resultado).isNotEmpty();
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }
}