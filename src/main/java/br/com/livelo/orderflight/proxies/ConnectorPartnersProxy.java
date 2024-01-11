package br.com.livelo.orderflight.proxies;

import br.com.livelo.orderflight.client.PartnerConnectorClient;
import br.com.livelo.partnersconfigflightlibrary.dto.WebhookDTO;
import br.com.livelo.partnersconfigflightlibrary.services.impl.PartnersConfigServiceImpl;
import br.com.livelo.partnersconfigflightlibrary.utils.Webhooks;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@AllArgsConstructor
public class ConnectorPartnersProxy {
    private final PartnersConfigServiceImpl partnersConfigService;
    private final PartnerConnectorClient partnerConnectorClient;

//    public OrderEntity getOrder(String partnerCode) {
    public void getOrder(String partnerCode) {
        WebhookDTO webhook = partnersConfigService.getPartnerWebhook(partnerCode, Webhooks.CONFIRMATION);
        final URI connectorUri = URI.create(webhook.getConnectorUrl());

        // var test = partnerConnectorClient.partnerConnectorConfirm(connectorUri);

    }
}
