package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.service.VendaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*", maxAge = 360)
public class WebhookController {

    private final VendaService vendaService;

    public WebhookController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PutMapping("/pagamento/{codigoPagamento}")
    public ResponseEntity<String> receberNotificacaoPagamento(
            @PathVariable String codigoPagamento,
            @RequestBody Map<String, String> payload) {

        String statusPagamento = payload.get("pagamentoStatus");

        if (statusPagamento == null || codigoPagamento == null) {
            return ResponseEntity.badRequest().body("Payload inválido ou código de pagamento ausente.");
        }

        try {
            vendaService.notificationProcessPayment(codigoPagamento, statusPagamento);

            System.out.println("Webhook de pagamento recebido para " + codigoPagamento + " com status: " + statusPagamento);
            return ResponseEntity.ok("Notificação de pagamento processada com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao processar webhook para " + codigoPagamento + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar notificação.");
        }
    }
}