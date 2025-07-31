package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.VendaService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/webhook")
@CrossOrigin(origins = "*", maxAge = 360)
@Slf4j
public class WebhookControllerRefatorado {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final VendaService vendaService;
    private final ConsultaVeiculoService consultaVeiculoService;

    public WebhookControllerRefatorado(VendaService vendaService, ConsultaVeiculoService consultaVeiculoService) {
        this.vendaService = vendaService;
        this.consultaVeiculoService = consultaVeiculoService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader)
            throws EventDataObjectDeserializationException {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Assinatura do webhook inválida: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Assinatura inválida");
        }

        log.info("Webhook recebido. Tipo: {}", event.getType());

        StripeObject stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
        logEventPayload(event);

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted((Session) stripeObject);
                break;
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
                break;
            case "checkout.session.async_payment_succeeded":
                log.info("Pagamento assíncrono bem-sucedido. Session ID: {}", ((Session) stripeObject).getId());
                break;
            case "checkout.session.async_payment_failed":
                log.warn("Pagamento assíncrono falhou. Session ID: {}", ((Session) stripeObject).getId());
                break;
            default:
                log.debug("Evento não tratado: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processado");
    }

    private void logEventPayload(Event event) {
        try {
            log.debug("Payload completo: ID={}, Tipo={}, Payload={} ", event.getId(), event.getType(), event.getData().toJson());
        } catch (Exception e) {
            log.error("Erro ao imprimir payload: {}", e.getMessage());
        }
    }

    private void handleCheckoutSessionCompleted(Session session) {
        log.info("Checkout session concluída. ID: {}, Status: {}", session.getId(), session.getPaymentStatus());

        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId == null) {
            log.error("Checkout Session {} sem PaymentIntent. Ignorado.", session.getId());
            return;
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String vendaIdStr = paymentIntent.getMetadata().get("venda_id");

            if (vendaIdStr == null) {
                log.error("Metadado 'venda_id' não encontrado. Session: {}", session.getId());
                return;
            }

            atualizarVenda(vendaIdStr, session.getId(), session.getPaymentStatus());
        } catch (StripeException e) {
            log.error("Erro ao buscar PaymentIntent {}: {}", paymentIntentId, e.getMessage());
        }
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        String vendaIdStr = paymentIntent.getMetadata().get("venda_id");
        if (vendaIdStr == null) {
            log.error("Metadado 'venda_id' ausente no PaymentIntent ID: {}", paymentIntent.getId());
            return;
        }

        atualizarVenda(vendaIdStr, paymentIntent.getId(), "paid");
    }

    private void atualizarVenda(String vendaIdStr, String stripeId, String statusPagamento) {
        try {
            UUID vendaId = UUID.fromString(vendaIdStr);
            Optional<VendaModel> vendaOptional = vendaService.findByVendaId(vendaId);

            if (vendaOptional.isEmpty()) {
                log.warn("Venda ID {} não encontrada.", vendaId);
                return;
            }

            VendaModel venda = vendaOptional.get();
            if (venda.getPagamentoStatus() == PagamentoStatus.EFETUADO) {
                log.info("Venda {} já aprovada.", venda.getVendaId());
                return;
            }

            if ("paid".equals(statusPagamento)) {
                venda.setPagamentoStatus(PagamentoStatus.EFETUADO);
            } else {
                venda.setPagamentoStatus(PagamentoStatus.RECUSADO);
            }
            venda.setStripeCheckoutSessionId(stripeId);
            vendaService.saveVenda(venda);

            log.info("Venda {} atualizada. Status: {}", venda.getVendaId(), venda.getPagamentoStatus());

            if (venda.getVeiculoId() != null) {
                boolean atualizado = consultaVeiculoService.atualizarStatusVeiculo(venda.getVeiculoId());
                if (atualizado) {
                    log.info("Veículo {} marcado como vendido.", venda.getVeiculoId());
                } else {
                    log.error("Falha ao atualizar status do veículo {}.", venda.getVeiculoId());
                }
            } else {
                log.warn("Venda {} sem Veículo ID para atualização.", venda.getVendaId());
            }
        } catch (IllegalArgumentException e) {
            log.error("UUID inválido: {} - Erro: {}", vendaIdStr, e.getMessage());
        }
    }
}
