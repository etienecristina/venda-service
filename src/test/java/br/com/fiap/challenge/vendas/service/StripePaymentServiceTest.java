package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.exception.CustomServiceException;
import br.com.fiap.challenge.vendas.model.VendaModel;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    @Mock
    private ConsultaVeiculoService consultaVeiculoService;

    @InjectMocks
    private StripePaymentService stripePaymentService;

    private final UUID vendaId = UUID.randomUUID();
    private final UUID veiculoId = UUID.randomUUID();
    private VendaModel vendaModel;
    private VeiculoDto veiculoDto;
    private static final String FAKE_STRIPE_PRODUCT_ID = "prod_fake123";
    private static final String FAKE_STRIPE_PRICE_ID = "price_fake123";
    private static final String FAKE_STRIPE_PAYMENT_LINK_URL = "https://checkout.stripe.com/pay/fake_url";

    @BeforeEach
    void setUp() {
        vendaModel =  new VendaModel();
        vendaModel.setVendaId(vendaId);
        vendaModel.setVeiculoId(veiculoId);

        veiculoDto = new VeiculoDto();
        veiculoDto.setVeiculoId(veiculoId);
        veiculoDto.setPreco(50000.00f);
        veiculoDto.setModelo("Onix");
        veiculoDto.setMarca("Chevrolet");
    }

    @Test
    void shouldReturnPaymentLinkUrl_whenVendaIsValid() throws StripeException, CustomServiceException {
        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.of(veiculoDto));

        try (MockedStatic<Stripe> mockedStripe = mockStatic(Stripe.class);
             MockedStatic<Product> mockedProduct = mockStatic(Product.class);
             MockedStatic<Price> mockedPrice = mockStatic(Price.class);
             MockedStatic<PaymentLink> mockedPaymentLink = mockStatic(PaymentLink.class)) {

            Product mockProduct = mock(Product.class);
            when(mockProduct.getId()).thenReturn(FAKE_STRIPE_PRODUCT_ID);
            when(Product.create(any(ProductCreateParams.class))).thenReturn(mockProduct);

            Price mockPrice = mock(Price.class);
            when(mockPrice.getId()).thenReturn(FAKE_STRIPE_PRICE_ID);
            when(Price.create(any(PriceCreateParams.class))).thenReturn(mockPrice);

            PaymentLink mockPaymentLink = mock(PaymentLink.class);
            when(mockPaymentLink.getUrl()).thenReturn(FAKE_STRIPE_PAYMENT_LINK_URL);
            when(PaymentLink.create(any(PaymentLinkCreateParams.class))).thenReturn(mockPaymentLink);

            String paymentLinkUrl = stripePaymentService.createStripePaymentLinkForVenda(vendaModel);

            assertEquals(FAKE_STRIPE_PAYMENT_LINK_URL, paymentLinkUrl);

            mockedProduct.verify(() -> Product.create(any(ProductCreateParams.class)));
            mockedPrice.verify(() -> Price.create(any(PriceCreateParams.class)));
            mockedPaymentLink.verify(() -> PaymentLink.create(any(PaymentLinkCreateParams.class)));
        }
    }

    @Test
    void shouldThrowException_whenVeiculoNotFound() {
        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.empty());

        CustomServiceException exception = assertThrows(CustomServiceException.class, () ->
                stripePaymentService.createStripePaymentLinkForVenda(vendaModel));

        assertEquals("Veículo com ID " + veiculoId + " não encontrado para criar pagamento.", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verifyNoInteractions(mock(Product.class), mock(Price.class), mock(PaymentLink.class));
    }

    @Test
    void shouldThrowException_whenVeiculoPriceIsInvalid() {

        veiculoDto.setPreco(0.0f);
        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.of(veiculoDto));

        CustomServiceException exception = assertThrows(CustomServiceException.class, () ->
                stripePaymentService.createStripePaymentLinkForVenda(vendaModel));

        assertEquals("Preço do veículo inválido para criar pagamento Stripe.", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verifyNoInteractions(mock(Product.class), mock(Price.class), mock(PaymentLink.class));
    }

    @Test
    void shouldPropagateStripeException_whenStripeCallFails() {

        when(consultaVeiculoService.findByVeiculoId(veiculoId)).thenReturn(Optional.of(veiculoDto));

        try (MockedStatic<Product> mockedProduct = mockStatic(Product.class)) {
            mockedProduct.when(() -> Product.create(any(ProductCreateParams.class)))
                    .thenThrow(new InvalidRequestException("Error de Stripe simulado", "param", "request_id", "code", 500, null));

            assertThrows(StripeException.class, () ->
                    stripePaymentService.createStripePaymentLinkForVenda(vendaModel));
        }
    }
}
