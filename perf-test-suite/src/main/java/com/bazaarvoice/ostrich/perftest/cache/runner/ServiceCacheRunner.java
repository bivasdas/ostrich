package com.bazaarvoice.ostrich.perftest.cache.runner;

import com.bazaarvoice.ostrich.MultiThreadedServiceFactory;
import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.cache.factory.SimpleResultFactory;
import com.bazaarvoice.ostrich.perftest.cache.factory.SimpleServiceFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.runner.ServiceRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.runner.ServicePoolRunner;
import com.bazaarvoice.ostrich.pool.ServiceCache;
import com.bazaarvoice.ostrich.pool.ServiceCacheBuilder;
import com.bazaarvoice.ostrich.pool.ServiceHandleHelper;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * This needs to be in the com.bazaarvoice.ostrich.pool package so that it can have direct access to ServiceCache
 * <p/>
 * This instantiates a service cache, creates threads to run test on and exposes various metrics for monitoring
 */
@SuppressWarnings ("deprecation")
public class ServiceCacheRunner extends ServiceRunner {

    private final ServiceCache<Service<String, String>> _serviceCache;
    private final ServiceHandleHelper _serviceHandleHelper;
    private final MultiThreadedServiceFactory<Service<String, String>> _serviceFactory;
    private final Timer _serviceTimer;

    /**
     * @param arguments      the command line arguments
     */
    public ServiceCacheRunner(Arguments arguments) {
        super(arguments);
        _serviceFactory = SimpleServiceFactory.newInstance();
        _serviceCache = new ServiceCacheBuilder<Service<String, String>>()
                .withServiceFactory(_serviceFactory)
                .withCachingPolicy(_cachingPolicy).build();
        _serviceHandleHelper = new ServiceHandleHelper(_serviceCache, SimpleResultFactory.newInstance());
        _serviceTimer = MetricsUtility.newTimer(ServicePoolRunner.class, "service-timer");
    }

    public ServiceCache<Service<String, String>> getServiceCache() {
        return _serviceCache;
    }

    public MultiThreadedServiceFactory<Service<String, String>> getServiceFactory() {
        return _serviceFactory;
    }

    @Override
    public void close() {
        Utilities.closeParallel(_serviceCache);
    }

    @Override
    protected void generateWorker() {
        Timer.Context clock = _serviceTimer.time();
        try {
            String work = RandomStringUtils.random(_workSize);
            String hashName = HashFunction.getRandomHashName();
            ServiceEndPoint serviceEndPoint = Utilities.buildServiceEndPoint(hashName);
            _serviceHandleHelper.serviceExecution(serviceEndPoint, work);
        } catch(Exception ignored) {}
        finally {
            clock.stop();
        }
    }
}
