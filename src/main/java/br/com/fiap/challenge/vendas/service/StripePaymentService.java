package br.com.fiap.challenge.vendas.service;

import br.com.fiap.challenge.vendas.dto.VeiculoDto;
import br.com.fiap.challenge.vendas.exception.CustomServiceException;
import br.com.fiap.challenge.vendas.model.VendaModel;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.PaymentLinkCreateParams.PaymentIntentData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class StripePaymentService {

    private final ConsultaVeiculoService consultaVeiculoService;

    public StripePaymentService(ConsultaVeiculoService consultaVeiculoService) {
        this.consultaVeiculoService = consultaVeiculoService;
    }

    public String createStripePaymentLinkForVenda(VendaModel vendaModel) throws StripeException, CustomServiceException {

        Optional<VeiculoDto> veiculoOptional = consultaVeiculoService.findByVeiculoId(vendaModel.getVeiculoId());
        if (veiculoOptional.isEmpty()) {
            throw new CustomServiceException("Veículo com ID " + vendaModel.getVeiculoId() + " não encontrado para criar pagamento.", HttpStatus.NOT_FOUND);
        }
        VeiculoDto veiculo = veiculoOptional.get();

        if (veiculo.getPreco() == null || veiculo.getPreco() <= 0) {
            throw new CustomServiceException("Preço do veículo inválido para criar pagamento Stripe.", HttpStatus.BAD_REQUEST);
        }

        long unitAmountCents = Math.round(veiculo.getPreco() * 100);

        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName(veiculo.getModelo())
                .setDescription("Venda de veículo: " + veiculo.getModelo() + " (" + veiculo.getMarca() + ")")
                .build();
        Product product = Product.create(productParams);

        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setUnitAmount(unitAmountCents)
                .setCurrency("brl")
                .setProduct(product.getId())
                .build();
        Price price = Price.create(priceParams);

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("venda_id", vendaModel.getVendaId().toString());

        PaymentIntentData paymentIntentData = PaymentIntentData.builder()
                .putAllMetadata(metadataMap)
                .build();

        PaymentLinkCreateParams paymentLinkParams = PaymentLinkCreateParams.builder()
                .addLineItem(
                        PaymentLinkCreateParams.LineItem.builder()
                                .setPrice(price.getId())
                                .setQuantity(1L)
                                .build()
                )
                .setAfterCompletion(
                        PaymentLinkCreateParams.AfterCompletion.builder()
                                .setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
                                .setRedirect(PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
                                        .setUrl("https://example.com/sucesso-venda/" + vendaModel.getVendaId().toString())
                                        .build())
                                .build()
                )
                .setPaymentIntentData(paymentIntentData)
                .build();

        PaymentLink paymentLink = PaymentLink.create(paymentLinkParams);

        return paymentLink.getUrl();
    }
}