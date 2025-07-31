package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.VendaService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendaService vendaService;

    @MockBean
    private ConsultaVeiculoService consultaVeiculoService;

    private final String payload = "{}";
    private final String signature = "t=123,v1=assinatura";

    @Test
    void deveRetornarOkQuandoWebhookCheckoutSessionCompleted() throws Exception {

        Event mockEvent = mock(Event.class);
        Mockito.when(mockEvent.getType()).thenReturn("checkout.session.completed");

        Session mockSession = new Session();
        mockSession.setId("sess_123");
        mockSession.setPaymentStatus("paid");
        mockSession.setPaymentIntent("pi_123");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        Mockito.when(mockDeserializer.deserializeUnsafe()).thenReturn(mockSession);
        Mockito.when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(any(), any(), any()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/webhook")
                            .content(payload)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", signature))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processado"));
        }
    }

    @Test
    void deveRetornarBadRequestQuandoAssinaturaInvalida() throws Exception {
        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(any(), any(), any()))
                    .thenThrow(new SignatureVerificationException("Assinatura inválida", null));

            mockMvc.perform(post("/webhook")
                            .content(payload)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", signature))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Assinatura inválida"));
        }
    }
}
