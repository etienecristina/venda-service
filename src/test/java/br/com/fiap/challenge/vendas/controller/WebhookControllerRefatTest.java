package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.VendaService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerRefatTest {

    private MockMvc mockMvc;

    @InjectMocks
    private WebhookControllerRefat webhookController;

    @Mock
    private VendaService vendaService;

    @Mock
    private ConsultaVeiculoService consultaVeiculoService;

    private final String webhookSecret = "whsec_test_secret";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
        ReflectionTestUtils.setField(webhookController, "webhookSecret", webhookSecret);
    }

    private String generateStripeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            long timestamp = System.currentTimeMillis() / 1000L;
            String signedPayload = timestamp + "." + payload;
            byte[] hmacSha256 = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacSha256) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return "t=" + timestamp + ",v1=" + hexString;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar assinatura do Stripe", e);
        }
    }


    @Test
    @DisplayName("Deve retornar 400 Bad Request se a assinatura for inválida")
    void deveRetornarBadRequestComAssinaturaInvalida() throws Exception {
        // GIVEN
        String payload = "{\"id\":\"evt_test\", \"type\":\"checkout.session.completed\"}";
        String sigHeader = "t=12345,v1=invalid_signature";

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException(sigHeader, payload));

            mockMvc.perform(post("/webhook")
                            .header("Stripe-Signature", sigHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Assinatura inválida"));

            verifyNoInteractions(vendaService, consultaVeiculoService);
        }
    }
}