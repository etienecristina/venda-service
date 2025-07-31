package br.com.fiap.challenge.vendas.model;


import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VendaModelTest {

    @Test
    @DisplayName("Deve criar e validar todos os atributos de VendaModel")
    void deveCriarEValidarAtributosDeVendaModel() {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        String cpfComprador = "12345678900";
        LocalDateTime dataVenda = LocalDateTime.now();
        PagamentoStatus pagamentoStatus = PagamentoStatus.PENDENTE;
        String stripeCheckoutSessionId = "cs_test_123";
        String stripePaymentLinkUrl = "https://checkout.stripe.com/c/pay/test_link";

        // WHEN
        VendaModel vendaModel = new VendaModel();
        vendaModel.setVendaId(vendaId);
        vendaModel.setVeiculoId(veiculoId);
        vendaModel.setCpfComprador(cpfComprador);
        vendaModel.setData_venda(dataVenda);
        vendaModel.setPagamentoStatus(pagamentoStatus);
        vendaModel.setStripeCheckoutSessionId(stripeCheckoutSessionId);
        vendaModel.setStripePaymentLinkUrl(stripePaymentLinkUrl);

        // THEN
        assertThat(vendaModel.getVendaId()).isEqualTo(vendaId);
        assertThat(vendaModel.getVeiculoId()).isEqualTo(veiculoId);
        assertThat(vendaModel.getCpfComprador()).isEqualTo(cpfComprador);
        assertThat(vendaModel.getData_venda()).isEqualTo(dataVenda);
        assertThat(vendaModel.getPagamentoStatus()).isEqualTo(pagamentoStatus);
        assertThat(vendaModel.getStripeCheckoutSessionId()).isEqualTo(stripeCheckoutSessionId);
        assertThat(vendaModel.getStripePaymentLinkUrl()).isEqualTo(stripePaymentLinkUrl);
    }
}
