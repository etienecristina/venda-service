package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.enums.VeiculoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ConsultaVeiculoService {
    Page<VeiculoDto> listarVeiculosPorStatus(VeiculoStatus status, Pageable pageable);
    Optional<VeiculoDto> findByVeiculoId(UUID veiculoId);
    boolean atualizarStatusVeiculo(UUID veiculoId);
}