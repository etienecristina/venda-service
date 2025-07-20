package br.com.fiap.challenge.vendas.controller;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.dto.VendaDto;
import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.service.ConsultaVeiculoService;
import br.com.fiap.challenge.vendas.service.VendaService;
import jakarta.validation.Valid;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/vendas")
@CrossOrigin(origins = "*", maxAge = 360)
public class VendaController {

    private final VendaService vendaService;
    private final ConsultaVeiculoService consultaVeiculoService;

    public VendaController(VendaService vendaService, ConsultaVeiculoService consultaVeiculoService) {
        this.vendaService = vendaService;
        this.consultaVeiculoService = consultaVeiculoService;
    }

    @PostMapping
    public ResponseEntity<Void> salvarVenda(@RequestBody @Valid VendaDto vendaDto) {
        var vendaModel = new VendaModel();

        BeanUtils.copyProperties(vendaDto, vendaModel);
        vendaModel.setData_venda(LocalDateTime.now(ZoneId.of("UTC")));
        vendaModel.setPagamentoStatus(PagamentoStatus.PENDENTE);

        var result = vendaService.saveVenda(vendaModel);

        return ResponseEntity.created(URI.create("/vendas/" + result.getVendaId())).build();
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

    @PatchMapping("/{vendaId}")
    public ResponseEntity<Object> cancelarVenda(@RequestBody @Valid VendaDto vendaDto) {
        var vendaOptional = vendaService.findByVendaId(vendaDto.getVeiculoId());

        if (vendaOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Venda não encontrada.");
        }

        var vendaModel = vendaOptional.get();
        vendaModel.setPagamentoStatus(PagamentoStatus.CANCELADO);
        vendaService.saveVenda(vendaModel);

        return ResponseEntity.status(HttpStatus.OK).body("Venda cancelada.");
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

        return (ResponseEntity.status(HttpStatus.OK).body(vendaService.findByVendaId(vendaId)));
    }
}