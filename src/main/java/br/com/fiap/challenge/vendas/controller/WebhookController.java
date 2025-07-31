//package br.com.fiap.challenge.vendas.controller;
//
//import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
//import br.com.fiap.challenge.vendas.model.VendaModel;
//import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
//import br.com.fiap.challenge.vendas.service.VendaService;
//import com.stripe.exception.EventDataObjectDeserializationException;
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.Event;
//import com.stripe.model.PaymentIntent;
//import com.stripe.model.StripeObject;
//import com.stripe.model.checkout.Session;
//import com.stripe.net.Webhook;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/webhook")
//@CrossOrigin(origins = "*", maxAge = 360)
//@Slf4j
//public class WebhookController {
//    @Value("${stripe.webhook.secret}")
//    private String webhookSecret;
//    private final VendaService vendaService;
//    private final ConsultaVeiculoService consultaVeiculoService;
//
//    public WebhookController(VendaService vendaService, ConsultaVeiculoService consultaVeiculoService) {
//        this.vendaService = vendaService;
//        this.consultaVeiculoService = consultaVeiculoService;
//    }
//
//    @PostMapping
//    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
//                                                      @RequestHeader("Stripe-Signature") String sigHeader) throws EventDataObjectDeserializationException {
//        Event event;
//
//        try {
//            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
//        } catch (SignatureVerificationException e) {
//            log.error("⚠️ Erro na verificação da assinatura do webhook: {}", e.getMessage());
//            return new ResponseEntity<>("Assinatura inválida", HttpStatus.BAD_REQUEST);
//        }
//
//        log.info("✅ Webhook recebido com sucesso! Tipo: {}", event.getType());
//
//        StripeObject stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
//
//        try {
//            log.debug("Payload completo do evento (ID: {}, Tipo: {}):\n{}", event.getId(), event.getType(), event.getData().toJson());
//        } catch (Exception e) {
//            log.error("Erro ao imprimir payload do evento (provavelmente devido a objetos não serializáveis): {}", e.getMessage());
//        }
//
//            if("checkout.session.completed".equals(event.getType())) {
//                Session session = (Session) stripeObject;
//                log.info("🎉 Checkout Session Concluída! ID da Sessão: {}", session.getId());
//                log.info("Status do Pagamento da Sessão: {}", session.getPaymentStatus());
//
//                String vendaIdStrSession = null;
//                String paymentIntentIdFromSession = session.getPaymentIntent();
//
//                if (paymentIntentIdFromSession != null) {
//                    try {
//                        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentIdFromSession);
//                        Map<String, String> metadata = paymentIntent.getMetadata();
//
//                        if (metadata != null && metadata.containsKey("venda_id")) {
//                            vendaIdStrSession = metadata.get("venda_id");
//                            log.info("✅ Metadado 'venda_id' encontrado no PaymentIntent (via fetch da Checkout Session): {}", vendaIdStrSession);
//                        } else {
//                            log.warn("Metadado 'venda_id' não encontrado no PaymentIntent (via fetch da Checkout Session) para a Checkout Session {}", session.getId());
//                        }
//
//                        if (vendaIdStrSession != null) {
//                            try {
//                                UUID vendaId = UUID.fromString(vendaIdStrSession);
//                                Optional<VendaModel> vendaOptional = vendaService.findByVendaId(vendaId);
//
//                                if (vendaOptional.isPresent()) {
//                                    VendaModel venda = vendaOptional.get();
//                                    if (venda.getPagamentoStatus() != PagamentoStatus.EFETUADO) {
//                                        if ("paid".equals(session.getPaymentStatus())) {
//                                            venda.setPagamentoStatus(PagamentoStatus.EFETUADO);
//                                            venda.setStripeCheckoutSessionId(session.getId());
//
//                                            vendaService.saveVenda(venda);
//                                            log.info("Venda {} atualizada para status APROVADO via checkout.session.completed.", venda.getVendaId());
//
//                                            if (venda.getVeiculoId() != null) {
//                                                boolean sucessoAtualizacaoVeiculo = consultaVeiculoService.atualizarStatusVeiculo(venda.getVeiculoId());
//                                                if (sucessoAtualizacaoVeiculo) {
//                                                    log.info("✅ Status do veículo {} atualizado para VENDIDO com sucesso via checkout.session.completed!", venda.getVeiculoId());
//                                                } else {
//                                                    log.error("❌ Falha ao atualizar o status do veículo {} via checkout.session.completed.", venda.getVeiculoId());
//                                                }
//                                            } else {
//                                                log.warn("Venda {} não possui Veículo ID associado para atualização de status via checkout.session.completed.", venda.getVendaId());
//                                            }
//                                        } else {
//                                            venda.setPagamentoStatus(PagamentoStatus.RECUSADO); // Ou outro status apropriado
//                                            venda.setStripeCheckoutSessionId(session.getId());
//                                            vendaService.saveVenda(venda);
//                                            log.warn("Pagamento da venda {} não foi 'paid' na checkout.session.completed. Status: {}", venda.getVendaId(), session.getPaymentStatus());
//                                        }
//                                    } else {
//                                        log.info("Venda {} já estava APROVADA na checkout.session.completed. Nenhum update necessário.", venda.getVendaId());
//                                    }
//                                } else {
//                                    log.warn("Venda com ID {} não encontrada no sistema para atualização via webhook (checkout.session.completed). Checkout Session ID: {}", vendaIdStrSession, session.getId());
//                                }
//                            } catch (IllegalArgumentException e) {
//                                log.error("Erro ao converter vendaId de String para UUID na checkout.session.completed: {}. Erro: {}", vendaIdStrSession, e.getMessage());
//                            }
//                        } else {
//                            log.error("CRÍTICO: 'venda_id' não obtido para Checkout Session {}. Venda NÃO ATUALIZADA.", session.getId());
//                        }
//                    } catch (StripeException e) {
//                        log.error("❌ Erro ao buscar PaymentIntent {} na API do Stripe (via checkout.session.completed): {}", paymentIntentIdFromSession, e.getMessage());
//                        return new ResponseEntity<>("Erro ao buscar PaymentIntent", HttpStatus.INTERNAL_SERVER_ERROR);
//                    }
//                } else {
//                    log.error("CRÍTICO: PaymentIntent ID não disponível na Checkout Session {}. Não foi possível buscar metadados para atualização.", session.getId());
//                }
//            }
//
//        else if("payment_intent.succeeded".equals(event.getType())) {
//            PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
//            log.info("💰 PaymentIntent Sucedido! ID: {}", paymentIntent.getId());
//
//            Map<String, String> metadata = paymentIntent.getMetadata();
//            String vendaIdStrPI = null;
//
//            if (metadata != null && metadata.containsKey("venda_id")) {
//                vendaIdStrPI = metadata.get("venda_id");
//                log.info("✅ Metadado 'venda_id' encontrado no PaymentIntent (succeeded event): {}", vendaIdStrPI);
//
//                if (vendaIdStrPI != null) {
//                    try {
//                        UUID vendaId = UUID.fromString(vendaIdStrPI);
//                        Optional<VendaModel> vendaOptional = vendaService.findByVendaId(vendaId);
//
//                        if (vendaOptional.isPresent()) {
//                            VendaModel venda = vendaOptional.get();
//
//                            if (venda.getPagamentoStatus() != PagamentoStatus.EFETUADO) {
//                                venda.setPagamentoStatus(PagamentoStatus.EFETUADO);
//                                // RECOMENDAÇÃO: Se seu campo 'paymentId' na VendaModel é do tipo Long,
//                                // você deve mudá-lo para String para armazenar o ID do PaymentIntent (ex: "pi_xxxxxxxx").
//                                // Caso contrário, a linha abaixo causará um erro de ClassCastException.
//                                // Por enquanto, esta linha está comentada para evitar o erro, mas o ideal é que você a use.
//                                // venda.setPaymentId(paymentIntent.getId());
//
//                                // Se você também precisa do StripeCheckoutSessionId neste ponto, e ele não está em VendaModel,
//                                // você teria que buscá-lo se fosse necessário. Por enquanto, focamos no PI ID.
//                                // Se o campo stripeCheckoutSessionId for String, e quiser colocar o PI ID aqui para consistência:
//                                // venda.setStripeCheckoutSessionId(paymentIntent.getId());
//
//                                vendaService.saveVenda(venda);
//                                log.info("Venda {} atualizada para status APROVADO via payment_intent.succeeded.", venda.getVendaId());
//
//
//                                // --- NOVO: CHAMADA PARA ATUALIZAR STATUS DO VEÍCULO (SEM SEGUNDO PARÂMETRO) ---
//                                if (venda.getVeiculoId() != null) {
//                                    boolean sucessoAtualizacaoVeiculo = consultaVeiculoService.atualizarStatusVeiculo(venda.getVeiculoId());
//                                    if (sucessoAtualizacaoVeiculo) {
//                                        log.info("✅ Status do veículo {} atualizado para VENDIDO com sucesso via checkout.session.completed!", venda.getVeiculoId());
//                                    } else {
//                                        log.error("❌ Falha ao atualizar o status do veículo {} via checkout.session.completed.", venda.getVeiculoId());
//                                    }
//                                } else {
//                                    log.warn("Venda {} não possui Veículo ID associado para atualização de status via checkout.session.completed.", venda.getVendaId());
//                                }
//
//
//                            } else {
//                                log.info("Venda {} já estava APROVADA via payment_intent.succeeded. Nenhum update necessário.", venda.getVendaId());
//                            }
//                        } else {
//                            log.warn("Venda com ID {} não encontrada no sistema para atualização via webhook (payment_intent.succeeded).", vendaIdStrPI);
//                        }
//                    } catch (IllegalArgumentException e) {
//                        log.error("Erro ao converter vendaId de String para UUID no payment_intent.succeeded: {}. Erro: {}", vendaIdStrPI, e.getMessage());
//                    }
//                } else {
//                    log.error("CRÍTICO: 'venda_id' nulo após extração de metadados do PaymentIntent. ID: {}", paymentIntent.getId());
//                }
//            } else {
//                log.error("CRÍTICO: Metadados vazios ou 'venda_id' não encontrado no PaymentIntent do evento payment_intent.succeeded. ID: {}", paymentIntent.getId());
//            }
//        }
//
//        else if("checkout.session.async_payment_succeeded".equals(event.getType())) {
//
//            Session asyncSucceededSession = (Session) stripeObject;
//            log.info("✅ Pagamento assíncrono concluído para Checkout Session: {}", asyncSucceededSession.getId());
//            // Este evento pode ser usado para logs ou para transições de status intermediárias.
//            // A confirmação final (PagamentoStatus.EFETUADO) virá do payment_intent.succeeded.
//        }
//
//        else if("checkout.session.async_payment_failed".equals(event.getType())) {
//            Session asyncFailedSession = (Session) stripeObject;
//            log.warn("❌ Pagamento assíncrono falhou para Checkout Session: {}", asyncFailedSession.getId());
//            // Lógica para marcar a venda como RECUSADA/FALHA (se aplicável)
//            // É uma boa prática verificar se o PaymentIntent associado realmente falhou antes de atualizar.
//        }
//
//        else{
//            log.debug("Tipo de evento não tratado: {}", event.getType());
//        }
//
//
//        return new ResponseEntity<>("Webhook processado", HttpStatus.OK);
//    }
//}