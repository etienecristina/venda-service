package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.dto.VendaDto;
import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.StripePaymentService;
import br.com.fiap.challenge.vendas.service.VendaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendaController.class)
class VendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendaService vendaService;

    @MockBean
    private ConsultaVeiculoService consultaVeiculoService;

    @MockBean
    private StripePaymentService stripePaymentService;

    @Test
    @DisplayName("Deve criar uma venda e retornar status 201 Created")
    void deveCriarVendaComSucesso() throws Exception {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        VendaDto vendaDto = new VendaDto();
        vendaDto.setVeiculoId(veiculoId);
        vendaDto.setCpfComprador("12345678900");

        VeiculoDto veiculoDto = new VeiculoDto();
        veiculoDto.setVeiculoId(veiculoId);
        veiculoDto.setPreco(50000.0f);
        veiculoDto.setVeiculoStatus(VeiculoStatus.DISPONIVEL);

        VendaModel savedVenda = new VendaModel();
        savedVenda.setVendaId(UUID.randomUUID());
        savedVenda.setPagamentoStatus(PagamentoStatus.PENDENTE);
        savedVenda.setStripePaymentLinkUrl("http://stripe.com/paymentlink");

        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.of(veiculoDto));
        when(vendaService.saveVenda(any(VendaModel.class))).thenReturn(savedVenda);
        when(stripePaymentService.createStripePaymentLinkForVenda(any(VendaModel.class))).thenReturn("http://stripe.com/paymentlink");

        // WHEN & THEN
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendaId").isNotEmpty())
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.paymentLinkUrl").value("http://stripe.com/paymentlink"));

        verify(vendaService, times(2)).saveVenda(any(VendaModel.class));
    }

    @Test
    @DisplayName("Não deve criar venda se o veículo não existir e retornar 404 Not Found")
    void naoDeveCriarVendaComVeiculoInexistente() throws Exception {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        VendaDto vendaDto = new VendaDto();
        vendaDto.setVeiculoId(veiculoId);

        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Veículo não encontrado."));

        verify(vendaService, never()).saveVenda(any());
    }

    @Test
    @DisplayName("Deve listar todas as vendas e retornar 200 OK")
    void deveListarTodasAsVendas() throws Exception {
        // GIVEN
        VendaModel venda1 = new VendaModel();
        venda1.setVendaId(UUID.randomUUID());
        VendaModel venda2 = new VendaModel();
        venda2.setVendaId(UUID.randomUUID());

        when(vendaService.listAllVendas()).thenReturn(List.of(venda1, venda2));

        // WHEN & THEN
        mockMvc.perform(get("/vendas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Deve buscar venda por ID e retornar 200 OK")
    void deveBuscarVendaPorId() throws Exception {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        VendaModel vendaModel = new VendaModel();
        vendaModel.setVendaId(vendaId);

        when(vendaService.findByVendaId(vendaId)).thenReturn(Optional.of(vendaModel));

        // WHEN & THEN
        mockMvc.perform(get("/vendas/{vendaId}", vendaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendaId").value(vendaId.toString()));
    }

    @Test
    @DisplayName("Não deve encontrar venda por ID e retornar 404 Not Found")
    void naoDeveEncontrarVendaPorId() throws Exception {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        when(vendaService.findByVendaId(vendaId)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(get("/vendas/{vendaId}", vendaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venda não encontrada."));
    }

    @Test
    @DisplayName("Deve cancelar uma venda e retornar 200 OK")
    void deveCancelarVenda() throws Exception {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        VendaModel vendaModel = new VendaModel();
        vendaModel.setVendaId(vendaId);
        vendaModel.setPagamentoStatus(PagamentoStatus.PENDENTE);

        when(vendaService.findByVendaId(vendaId)).thenReturn(Optional.of(vendaModel));
        when(vendaService.saveVenda(any(VendaModel.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN & THEN
        mockMvc.perform(put("/vendas/{vendaId}/cancelar", vendaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Venda cancelada com sucesso."));
    }
}
