package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.codahale.metrics.health.HealthCheck;

public class HashServiceHealthCheck extends HealthCheck {

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
