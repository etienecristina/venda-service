package br.com.fiap.challenge.vendas.dto;

import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class VeiculoDto {
    private UUID veiculoId;
    private String marca;
    private String modelo;
    private Integer ano;
    private String cor;
    private Float preco;
    private VeiculoStatus veiculoStatus;
    private LocalDateTime dataDeInclusao;
    private LocalDateTime dataDeAtualizacao;
}