package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.bazaarvoice.curator.dropwizard.ZooKeeperConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.client.JerseyClientConfiguration;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class HashServiceConfiguration extends Configuration {

    @NotNull
    @Valid
    @JsonProperty
    private ZooKeeperConfiguration zooKeeper = new ZooKeeperConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

    public ZooKeeperConfiguration getZooKeeperConfiguration() {
        return zooKeeper;
    }

    public JerseyClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }
}
