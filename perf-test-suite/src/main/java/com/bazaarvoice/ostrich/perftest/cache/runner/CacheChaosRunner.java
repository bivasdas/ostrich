package com.bazaarvoice.ostrich.perftest.cache.runner;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.runner.ChaosRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.pool.ServiceCache;
import com.google.common.collect.Queues;

import java.util.Queue;

public class CacheChaosRunner extends ChaosRunner {

    private final ServiceCache<Service<String, String>> _serviceCache;
    private final Queue<ServiceEndPoint> _availableEndPoints = Queues.newConcurrentLinkedQueue();

    public CacheChaosRunner(Arguments arguments, ServiceCache<Service<String, String>> serviceCache) {
        super(arguments);
        _serviceCache = serviceCache;
    }

    @Override
    protected void doChaos() throws Exception {
        String hashName = HashFunction.getRandomHashName();
        ServiceEndPoint endPoint = Utilities.buildServiceEndPoint(hashName);
        _serviceCache.evict(endPoint);
        _availableEndPoints.add(endPoint);
    }

    @Override
    protected void doStable() throws Exception {
        ServiceEndPoint endPoint = _availableEndPoints.remove();
        _serviceCache.register(endPoint);
    }
}
