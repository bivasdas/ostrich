package com.bazaarvoice.ostrich.perftest.pool.web.client;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.service.HashService;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.codahale.metrics.Meter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import org.apache.http.client.HttpClient;

import javax.validation.Validation;
import java.net.URI;

public class HashServiceClientServiceFactory implements MultiThreadedServiceFactory<HashServiceClient> {
    private final Client _client;
    private final Meter _serviceCreated;
    private final Meter _serviceDestroyed;


    public HashServiceClientServiceFactory(HttpClientConfiguration configuration) {
        this(createDefaultJerseyClient(configuration));
    }

    public HashServiceClientServiceFactory(Client jerseyClient) {
        _client = jerseyClient;
        _serviceCreated = MetricsUtility.newMeter(HashServiceClientServiceFactory.class, "Service-Created");
        _serviceDestroyed = MetricsUtility.newMeter(HashServiceClientServiceFactory.class, "Service-Destroyed");
    }

    private static ApacheHttpClient4 createDefaultJerseyClient(HttpClientConfiguration configuration) {
        HttpClient httpClient = new HttpClientBuilder(MetricsUtility.getMetricsRegistry()).using(configuration).build("hashClient");
        ApacheHttpClient4Handler handler = new ApacheHttpClient4Handler(httpClient, null, true);
        ApacheHttpClient4Config config = new DefaultApacheHttpClient4Config();
        config.getSingletons().add(new JacksonMessageBodyProvider(Jackson.newObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator()));
        return new ApacheHttpClient4(handler, config);
    }

    @Override
    public String getServiceName() {
        return HashService.getServiceName();
    }

    @Override
    public void configure(ServicePoolBuilder<HashServiceClient> servicePoolBuilder) {
    }

    @Override
    public HashServiceClient create(ServiceEndPoint endPoint) {
        _serviceCreated.mark();
        return new HashServiceClient(endPoint, _client);
    }

    @Override
    public void destroy(ServiceEndPoint endPoint, HashServiceClient service) {
        _serviceDestroyed.mark();
    }

    @Override
    public boolean isRetriableException(Exception exception) {
        return false;
    }

    @Override
    public boolean isHealthy(ServiceEndPoint endPoint) {
        URI adminUrl = Utilities.getAdminUriFromPayload(endPoint.getPayload());
        return _client.resource(adminUrl).path("/healthcheck").head().getStatus() == 200;
    }
}
