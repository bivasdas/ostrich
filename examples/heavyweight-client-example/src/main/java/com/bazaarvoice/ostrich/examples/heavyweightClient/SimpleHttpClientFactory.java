package com.bazaarvoice.ostrich.examples.heavyweightClient;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.RequestBuilder;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;

public class SimpleHttpClientFactory<T> implements MultiThreadedServiceFactory<RequestBuilder> {

    private final Client client;

    public SimpleHttpClientFactory( final ClientConfig defaultClientConfig ) {
        this.client = Client.create(defaultClientConfig);
    }

    public SimpleHttpClientFactory() {
        this.client = Client.create();
    }

    @Override
    public String getServiceName() {
        return "HttpClientFactory";
    }

    @Override
    public void configure( final ServicePoolBuilder<RequestBuilder> servicePoolBuilder ) {
        // no-op
    }

    @Override
    public RequestBuilder create( final ServiceEndPoint endPoint ) {
        return client.resource( endPoint.getPayload() ).getRequestBuilder();
    }

    @Override
    public void destroy( final ServiceEndPoint endPoint, final RequestBuilder service ) {
        // no-op
    }

    @Override
    public boolean isHealthy( final ServiceEndPoint endPoint ) {

        return true;
    }

    @Override
    public boolean isRetriableException( final Exception exception ) {
        return false;
    }


}
