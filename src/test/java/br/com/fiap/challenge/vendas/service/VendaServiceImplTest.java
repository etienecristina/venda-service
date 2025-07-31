package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceImplTest {

    @InjectMocks
    private VendaServiceImpl vendaService;

    @Mock
    private VendaRepository vendaRepository;

    @Test
    @DisplayName("Deve salvar uma venda com sucesso")
    void deveSalvarVendaComSucesso() {
        // GIVEN
        VendaModel venda = new VendaModel();
        when(vendaRepository.save(any(VendaModel.class))).thenReturn(venda);

        // WHEN
        VendaModel resultado = vendaService.saveVenda(venda);

        // THEN
        assertThat(resultado).isEqualTo(venda);
        verify(vendaRepository, times(1)).save(venda);
    }

    @Test
    @DisplayName("Deve encontrar uma venda por ID")
    void deveEncontrarVendaPorId() {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        VendaModel venda = new VendaModel();
        when(vendaRepository.findById(vendaId)).thenReturn(Optional.of(venda));

        // WHEN
        Optional<VendaModel> resultado = vendaService.findByVendaId(vendaId);

        // THEN
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(venda);
        verify(vendaRepository, times(1)).findById(vendaId);
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando a venda não for encontrada")
    void deveRetornarOptionalVazioQuandoNaoEncontrarVenda() {
        // GIVEN
        UUID vendaId = UUID.randomUUID();
        when(vendaRepository.findById(vendaId)).thenReturn(Optional.empty());

        // WHEN
        Optional<VendaModel> resultado = vendaService.findByVendaId(vendaId);

        // THEN
        assertThat(resultado).isEmpty();
        verify(vendaRepository, times(1)).findById(vendaId);
    }

    @Test
    @DisplayName("Deve listar todas as vendas com sucesso")
    void deveListarTodasAsVendas() {
        // GIVEN
        VendaModel venda1 = new VendaModel();
        VendaModel venda2 = new VendaModel();
        List<VendaModel> vendas = List.of(venda1, venda2);
        when(vendaRepository.findAll()).thenReturn(vendas);

        // WHEN
        List<VendaModel> resultado = vendaService.listAllVendas();

        // THEN
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(venda1, venda2);
        verify(vendaRepository, times(1)).findAll();
    }
}