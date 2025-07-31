package br.com.fiap.challenge.vendas.model;

import br.com.fiap.challenge.vendas.enums.PagamentoStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TB_VENDAS")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID vendaId;
    @Column(nullable = false)
    private UUID veiculoId;
    @Column(nullable = false)
    private String cpfComprador;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime data_venda;
    private String codigoPagamento;
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PagamentoStatus pagamentoStatus;
    private String agreementId;
    @Column(unique = true)
    private Long paymentId;
    @Column(unique = true)
    private String stripeCheckoutSessionId;
    private String stripePaymentLinkUrl;

}