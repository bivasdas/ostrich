package com.bazaarvoice.ostrich.perftest.cache.runner;

import com.bazaarvoice.ostrich.perftest.cache.factory.SimpleServiceFactory;
import com.bazaarvoice.ostrich.perftest.core.runner.ChaosRunner;
import com.bazaarvoice.ostrich.perftest.core.runner.LoadRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;

/**
 * This class creates the service runner and runs the requested load as parsed via Arguments
 * This also prints/writes the report log and/or the statistics as requested in the Arguments
 */
public class CacheLoadRunner extends LoadRunner {

    private ServiceCacheRunner _serviceCacheRunner;

    public CacheLoadRunner(Arguments arguments) throws ClassNotFoundException {
        super(arguments);

        _serviceCacheRunner = new ServiceCacheRunner(arguments);
        CacheChaosRunner cacheChaosRunner = new CacheChaosRunner(arguments, _serviceCacheRunner.getServiceCache());

        _workers.addAll(_serviceCacheRunner.generateWorkers());
        _workers.addAll(cacheChaosRunner.generateChaosWorkers());

        for (Thread thread : _workers) {
            thread.start();
        }

        _allMetrics.put("Service-Created", MetricsUtility.get(SimpleServiceFactory.class, "Service-Created"));
        _allMetrics.put("Service-Destroyed", MetricsUtility.get(SimpleServiceFactory.class, "Service-Destroyed"));
        _allMetrics.put("Service-Time", MetricsUtility.get(SimpleServiceFactory.class, "Service-Time"));

        _allMetrics.put("Service-Executed", MetricsUtility.get(ServiceCacheRunner.class, "Service-Executed"));
        _allMetrics.put("Cache-Miss", MetricsUtility.get(ServiceCacheRunner.class, "Cache-Miss"));
        _allMetrics.put("Service-Failure", MetricsUtility.get(ServiceCacheRunner.class, "Service-Failure"));
        _allMetrics.put("Checkout", MetricsUtility.get(ServiceCacheRunner.class, "Checkout"));
        _allMetrics.put("CheckIn", MetricsUtility.get(ServiceCacheRunner.class, "CheckIn"));
        _allMetrics.put("Total-Exec", MetricsUtility.get(ServiceCacheRunner.class, "Total-Exec"));

        _allMetrics.put("Chaos", MetricsUtility.get(ChaosRunner.class, "Chaos"));
        _allMetrics.put("Stable", MetricsUtility.get(ChaosRunner.class, "Stable"));

        String serviceName = _serviceCacheRunner.getServiceFactory().getServiceName();
        if(arguments.useMultiThreadedCache()) {
            _allMetrics.put("eviction-time", MetricsUtility.get(
                    "com.bazaarvoice.ostrich.pool.MultiThreadedClientServiceCache", "eviction-time", serviceName));
            _allMetrics.put("register-time", MetricsUtility.get(
                    "com.bazaarvoice.ostrich.pool.MultiThreadedClientServiceCache", "register-time", serviceName));
        }
        else {
            _allMetrics.put("load-time", MetricsUtility.get(
                    "com.bazaarvoice.ostrich.pool.SingleThreadedClientServiceCache", "load-time", serviceName));
        }
    }

    @Override
    public void printHeaders() {
        long currentRuntime = Utilities.currentTimeSeconds() - _startTime;
        System.out.println(String.format(_headerDateTime, currentRuntime));
        if (_arguments.useMultiThreadedCache()) {
            System.out.println(_headerNewCache);
        } else {
            System.out.println(_headerOldCache);
        }
    }

    @Override
    public void close() {
        _serviceCacheRunner.close();
    }
}
