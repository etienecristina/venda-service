package br.com.fiap.challenge.vendas.dto;


import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VendaDtoTest {

    @Test
    @DisplayName("Deve criar e validar todos os atributos de VendaDto")
    void deveCriarEValidarAtributosDeVendaDto() {
        // GIVEN
        UUID veiculoId = UUID.randomUUID();
        String cpfComprador = "12345678900";
        LocalDateTime dataVenda = LocalDateTime.now();
        String codigoPagamento = "COD-PAGAMENTO-123";
        PagamentoStatus pagamentoStatus = PagamentoStatus.PENDENTE;

        // WHEN
        VendaDto vendaDto = new VendaDto();
        vendaDto.setVeiculoId(veiculoId);
        vendaDto.setCpfComprador(cpfComprador);
        vendaDto.setData_venda(dataVenda);
        vendaDto.setCodigoPagamento(codigoPagamento);
        vendaDto.setPagamentoStatus(pagamentoStatus);

        // THEN
        assertThat(vendaDto.getVeiculoId()).isEqualTo(veiculoId);
        assertThat(vendaDto.getCpfComprador()).isEqualTo(cpfComprador);
        assertThat(vendaDto.getData_venda()).isEqualTo(dataVenda);
        assertThat(vendaDto.getCodigoPagamento()).isEqualTo(codigoPagamento);
        assertThat(vendaDto.getPagamentoStatus()).isEqualTo(pagamentoStatus);
    }
}