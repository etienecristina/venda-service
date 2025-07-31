package br.com.fiap.challenge.vendas.service.impl;

import br.com.fiap.challenge.vendas.model.VendaModel;
import br.com.fiap.challenge.vendas.repository.VendaRepository;
import br.com.fiap.challenge.vendas.service.VendaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class VendaServiceImpl implements VendaService {

    private final VendaRepository vendaRepository;

    public VendaServiceImpl(VendaRepository vendaRepository) {
        this.vendaRepository = vendaRepository;
    }

    @Override
    public VendaModel saveVenda(VendaModel vendaModel) {
        return vendaRepository.save(vendaModel);
    }

    @Override
    public Optional<VendaModel> findByVendaId(UUID vendaId) {
        return vendaRepository.findById(vendaId);
    }

    @Override
    public List<VendaModel> listAllVendas() {
        return vendaRepository.findAll();
    }
}