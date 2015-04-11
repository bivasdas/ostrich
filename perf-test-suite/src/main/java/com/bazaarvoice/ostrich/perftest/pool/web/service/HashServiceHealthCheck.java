package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.yammer.metrics.core.HealthCheck;

public class HashServiceHealthCheck extends HealthCheck {

    public HashServiceHealthCheck() {
        super("service");
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
