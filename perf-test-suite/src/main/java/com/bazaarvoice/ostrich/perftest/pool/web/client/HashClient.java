package com.bazaarvoice.ostrich.perftest.pool.web.client;

import com.bazaarvoice.ostrich.ServiceCallback;
import com.bazaarvoice.ostrich.ServiceFactory;
import com.bazaarvoice.ostrich.ServicePool;
import com.bazaarvoice.ostrich.discovery.zookeeper.ZooKeeperHostDiscovery;
import com.bazaarvoice.ostrich.dropwizard.healthcheck.ContainsHealthyEndPointCheck;
import com.bazaarvoice.ostrich.exceptions.ServiceException;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.TransportData;
import com.bazaarvoice.ostrich.perftest.pool.web.service.HashService;
import com.bazaarvoice.ostrich.perftest.pool.web.service.HashServiceConfiguration;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicy;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.bazaarvoice.ostrich.retry.RetryNTimes;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.apache.curator.framework.CuratorFramework;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bazaarvoice.ostrich.perftest.core.utils.Utilities.getConfiguration;

public class HashClient implements Closeable {

    private final ServicePool<HashServiceClient> _calculatorPool;
    private final CuratorFramework _curatorFramework;
    private final Semaphore _lock;
    private int _actualCallRate;
    private int _callRateBase = 4096;
    private long _lastUpdated;

    private final Meter _totalClientCalled;
    private final Meter _actualClientCalled;
    private final Meter _noopClientCalled;
    private final Timer _executionTimer;

    private final AtomicInteger _increaseRequest;
    private final AtomicInteger _decreaseRequest;

    public HashClient(ServiceCachingPolicy cachingPolicy, File configYAMLFile) throws Exception {

        _totalClientCalled = MetricsUtility.newMeter(HashClient.class, "total-client-called");
        _actualClientCalled = MetricsUtility.newMeter(HashClient.class, "actual-client-called");
        _noopClientCalled = MetricsUtility.newMeter(HashClient.class, "noop-client-called");
        _executionTimer = MetricsUtility.newTimer(HashClient.class, "execution-timer");

        HashServiceConfiguration configuration = getConfiguration(HashServiceConfiguration.class, configYAMLFile, HashService.getServiceName());
        _curatorFramework = configuration.getZooKeeperConfiguration().newCurator();
        _curatorFramework.start();

        ServiceFactory<HashServiceClient> serviceFactory = new HashServiceClientServiceFactory(configuration.getHttpClientConfiguration());
        _calculatorPool = ServicePoolBuilder.create(HashServiceClient.class)
                .withServiceFactory(serviceFactory)
                .withHostDiscovery(new ZooKeeperHostDiscovery(_curatorFramework, serviceFactory.getServiceName(), MetricsUtility.getMetricsRegistry()))
                .withCachingPolicy(cachingPolicy)
                .withMetricRegistry(MetricsUtility.getMetricsRegistry())
                .build();

        new HealthCheckRegistry().register(HashService.getServiceName(), ContainsHealthyEndPointCheck.forPool(_calculatorPool));
        _actualCallRate = 1;
        _lock = new Semaphore(1);
        _increaseRequest = new AtomicInteger();
        _decreaseRequest = new AtomicInteger();
    }

    public TransportData call(final String function, final String data) {
        Timer.Context clock = _executionTimer.time();
        try {
            return _calculatorPool.execute(
                    new RetryNTimes(0),
                    new ServiceCallback<HashServiceClient, TransportData>() {
                        @Override
                        public TransportData call(HashServiceClient client) throws ServiceException {
                            _totalClientCalled.mark();
                            if (makeActualCall()) {
                                _actualClientCalled.mark();
                                return client.call(function, data);
                            } else {
                                _noopClientCalled.mark();
                                return client.callNoop(function, data);
                            }
                        }
                    });
        }
        finally {
            clock.stop();
        }
    }

    private boolean makeActualCall() {
        return Utilities.getRandomInt(_callRateBase) < _actualCallRate;
    }

    public void increaseCallRate(boolean doIt) {
        int requestNumber = _increaseRequest.incrementAndGet();
        if(_lock.tryAcquire()) {
            if (doIt &&
                    _lastUpdated + 1000 < System.currentTimeMillis() &&
                    _actualCallRate < _callRateBase &&
                    requestNumber % 1000 == 0) {

                _actualCallRate = _actualCallRate + 1;
                _lastUpdated = System.currentTimeMillis();
                _increaseRequest.set(0);
            }
            _lock.release();
        }
    }

    public void decreaseCallRate() {
        int requestNumber = _decreaseRequest.incrementAndGet();
        if (_lock.tryAcquire()) {
            if (_lastUpdated + 100 < System.currentTimeMillis() &&
                    _actualCallRate > 0 &&
                    requestNumber % 100 == 0) {

                _actualCallRate = _actualCallRate - 1;
                _lastUpdated = System.currentTimeMillis();
                _decreaseRequest.set(0);
            }
            _lock.release();
        }
    }

    public float getCallRatePercent() {
        return ((float)_actualCallRate/_callRateBase) * 100;
    }

    @Override
    public void close() {
        Utilities.closeParallel(_calculatorPool);
        Utilities.closeParallel(_curatorFramework);
    }
}
