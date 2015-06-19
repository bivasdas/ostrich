package com.bazaarvoice.ostrich.perftest.pool.web.client;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.core.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.TransportData;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class HashServiceClient {
    private final Client _client;
    private final UriBuilder _service;

    /**
     * note: this client is thread safe as both {@link com.sun.jersey.api.client.Client} &
     * {@link javax.ws.rs.core.UriBuilder} are both thread safe
     */
    public HashServiceClient(ServiceEndPoint endPoint, Client jerseyClient) {
        this(Utilities.getServiceUriFromPayload(endPoint.getPayload()), jerseyClient);
    }

    public HashServiceClient(URI endPoint, Client jerseyClient) {
        _client = checkNotNull(jerseyClient, "jerseyClient");
        _service = UriBuilder.fromUri(endPoint);
    }


    public TransportData call(String function, String data) {
        TransportData transportData = new TransportData(function, data);
        URI uri = _service.clone().segment("hash").build();
        ClientResponse response = _client
                .resource(uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, transportData);
        transportData = response.getEntity(TransportData.class).withUri(uri);
        response.close();
        return transportData;
    }

    public TransportData callNoop(String function, String data) {
        String output = HashFunction.valueOf(function).process(data);
        return new TransportData(function, data, output);
    }
}
