package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.dto.VendaDto;
import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.exception.CustomServiceException;
import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.StripePaymentService;
import br.com.fiap.challenge.vendas.service.VendaService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/vendas")
@CrossOrigin(origins = "*", maxAge = 360)
@Slf4j
public class VendaController {

    private final VendaService vendaService;
    private final ConsultaVeiculoService consultaVeiculoService;
    private final StripePaymentService stripePaymentService;


    public VendaController(VendaService vendaService, ConsultaVeiculoService consultaVeiculoService, StripePaymentService stripePaymentService) {
        this.vendaService = vendaService;
        this.consultaVeiculoService = consultaVeiculoService;
        this.stripePaymentService = stripePaymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> salvarVenda(@RequestBody @Valid VendaDto vendaDto) {
        var vendaModel = new VendaModel();
        Optional<VeiculoDto> veiculoOptional = consultaVeiculoService.findByVeiculoId(vendaDto.getVeiculoId());

        if (veiculoOptional.isEmpty()) {
            log.warn("Veículo com ID {} não encontrado.", vendaDto.getVeiculoId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Veículo não encontrado."));
        }

        var veiculo = veiculoOptional.get();

        if (VeiculoStatus.VENDIDO.equals(veiculo.getVeiculoStatus())) {
            log.warn("Tentativa de criar uma venda com veículo já vendido. Requisição rejeitada.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Requisição rejeitada. O veículo já foi vendido."));
        }

        BeanUtils.copyProperties(vendaDto, vendaModel);

        vendaModel.setData_venda(LocalDateTime.now(ZoneId.of("UTC")));
        vendaModel.setPagamentoStatus(PagamentoStatus.PENDENTE);

        VendaModel savedVenda = vendaService.saveVenda(vendaModel);

        String paymentLinkUrl = null;
        try {
            paymentLinkUrl = stripePaymentService.createStripePaymentLinkForVenda(savedVenda);
            savedVenda.setStripePaymentLinkUrl(paymentLinkUrl);
            vendaService.saveVenda(savedVenda);
            log.info("Link de pagamento Stripe gerado para a venda {}: {}", savedVenda.getVendaId(), paymentLinkUrl);
        } catch (StripeException | CustomServiceException e) {
            log.error("Erro ao gerar link de pagamento Stripe para a venda {}: {}", savedVenda.getVendaId(), e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("vendaId", savedVenda.getVendaId());
        response.put("statusPagamento", savedVenda.getPagamentoStatus());

        if (paymentLinkUrl != null) {
            response.put("paymentLinkUrl", paymentLinkUrl);
            response.put("mensagem", "Venda criada com sucesso! Acesse o 'paymentLinkUrl' para completar o pagamento.");
        } else {
            response.put("mensagem", "Venda criada, mas houve um erro ao gerar o link de pagamento.");
        }
        return ResponseEntity.created(URI.create("/vendas/" + savedVenda.getVendaId())).body(response);
    }

    @GetMapping("/veiculos-a-venda")
    public ResponseEntity<Page<VeiculoDto>> listarVeiculosAVenda(
            @PageableDefault(sort = "preco", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.status(HttpStatus.OK).body(consultaVeiculoService.listarVeiculosPorStatus(VeiculoStatus.DISPONIVEL, pageable));
    }

    @GetMapping("/veiculos-vendidos")
    public ResponseEntity<Page<VeiculoDto>> listarVeiculosVendidos(
            @PageableDefault(sort = "preco", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.status(HttpStatus.OK).body(consultaVeiculoService.listarVeiculosPorStatus(VeiculoStatus.VENDIDO, pageable));
    }

    @PutMapping("/{vendaId}/cancelar")
    public ResponseEntity<Object> cancelarVenda(@PathVariable UUID vendaId) {

        var vendaOptional = vendaService.findByVendaId(vendaId);

        if (vendaOptional.isEmpty()) {
            log.warn("Venda não encontrada. Venda ID: {}", vendaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Venda não encontrada.");
        }

        var vendaModel = vendaOptional.get();

        if (vendaModel.getPagamentoStatus() == PagamentoStatus.EFETUADO) {
            log.warn("Tentativa de cancelar venda com status EFETUADA. Venda ID: {}", vendaId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Não é possível cancelar uma venda que já foi EFETUADA.");
        }
        if (vendaModel.getPagamentoStatus() == PagamentoStatus.RECUSADO) {
            log.info("Tentativa de cancelar venda com status RECUSADA. Venda ID: {}", vendaId);
            return ResponseEntity.status(HttpStatus.OK).body("Venda cancelada.");
        }

        vendaModel.setPagamentoStatus(PagamentoStatus.CANCELADO);
        vendaService.saveVenda(vendaModel);

        log.info("Venda {} cancelada com sucesso. Status atualizado para CANCELADO.", vendaId);
        return ResponseEntity.status(HttpStatus.OK).body("Venda cancelada com sucesso.");
    }

    @PutMapping("/{vendaId}")
    public ResponseEntity<Object> editarVenda(@PathVariable("vendaId") UUID vendaId,
                                              @RequestBody @Valid VendaDto vendaDto) {

        Optional<VendaModel> vendaOptional = vendaService.findByVendaId(vendaId);

        if (vendaOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Venda não cadastrada.");
        }

        var vendaModel = vendaOptional.get();
        vendaModel.setVeiculoId(vendaDto.getVeiculoId());
        vendaModel.setCpfComprador(vendaDto.getCpfComprador());
        vendaModel.setCodigoPagamento(vendaDto.getCodigoPagamento());
        vendaModel.setData_venda(LocalDateTime.now(ZoneId.of("UTC")));
        vendaModel.setPagamentoStatus(vendaDto.getPagamentoStatus());

        return ResponseEntity.status(HttpStatus.OK).body(vendaService.saveVenda(vendaModel));
    }

    @GetMapping()
    public ResponseEntity<List<VendaModel>> listarTodosAsVendas() {
        return (ResponseEntity.status(HttpStatus.OK).body(vendaService.listAllVendas()));
    }

    @GetMapping("/{vendaId}")
    public ResponseEntity<Object> buscarVendaPorId(@PathVariable("vendaId") UUID vendaId) {

        var vendaOptional = vendaService.findByVendaId(vendaId);

        if (vendaOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Venda não encontrada.");
        }

        return (ResponseEntity.status(HttpStatus.OK).body(vendaOptional.get()));
    }
}