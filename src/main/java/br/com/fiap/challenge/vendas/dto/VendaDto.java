package br.com.fiap.challenge.vendas.dto;

import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class VendaDto {
    private UUID veiculoId;
    private String cpfComprador;
    private LocalDateTime data_venda;
    private String codigoPagamento;
    private PagamentoStatus pagamentoStatus;
}