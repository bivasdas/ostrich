package com.bazaarvoice.ostrich.pool;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.exceptions.NoCachedInstancesAvailableException;
import com.bazaarvoice.ostrich.perftest.cache.runner.ServiceCacheRunner;
import com.bazaarvoice.ostrich.perftest.core.Result;
import com.bazaarvoice.ostrich.perftest.core.ResultFactory;
import com.bazaarvoice.ostrich.perftest.core.Service;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

/**
 * This needs to be in the com.bazaarvoice.ostrich.pool package so that it can have direct access to ServiceCache
 * <p/>
 */
public class ServiceHandleHelper {

    private final ResultFactory<String> _resultFactory;
    private final ServiceCache<Service<String, String>> _serviceCache;

    private final Meter _serviceMeter;
    private final Meter _cacheMissMeter;
    private final Meter _failureMeter;
    private final Timer _checkoutTimer;
    private final Timer _checkinTimer;
    private final Timer _totalExecTimer;

    public ServiceHandleHelper(ServiceCache<Service<String, String>> serviceCache, ResultFactory<String> resultFactory) {
        _resultFactory = resultFactory;
        _serviceCache = serviceCache;

        _serviceMeter = MetricsUtility.newMeter(ServiceCacheRunner.class, "Service-Executed");
        _cacheMissMeter = MetricsUtility.newMeter(ServiceCacheRunner.class, "Cache-Miss");
        _failureMeter = MetricsUtility.newMeter(ServiceCacheRunner.class, "Service-Failure");
        _checkoutTimer = MetricsUtility.newTimer(ServiceCacheRunner.class, "Checkout");
        _checkinTimer = MetricsUtility.newTimer(ServiceCacheRunner.class, "CheckIn");
        _totalExecTimer = MetricsUtility.newTimer(ServiceCacheRunner.class, "Total-Exec");
    }

    public Result<String> serviceExecution(ServiceEndPoint serviceEndPoint, String work) {
        Timer.Context totalTimeContext = _totalExecTimer.time();
        try {
            Timer.Context checkoutTimeContext = _checkoutTimer.time();
            ServiceHandle<Service<String, String>> serviceHandle = _serviceCache.checkOut(serviceEndPoint);
            Service<String, String> service = serviceHandle.getService();
            checkoutTimeContext.stop();

            String result = service.process(work);

            Timer.Context checkInTimeContext = _checkinTimer.time();
            _serviceCache.checkIn(serviceHandle);
            checkInTimeContext.stop();
            _serviceMeter.mark();

            return _resultFactory.createResponse(result);
        } catch (NoCachedInstancesAvailableException exception) {
            _cacheMissMeter.mark();
            return _resultFactory.createResponse(exception);
        } catch (Exception exception) {
            _failureMeter.mark();
            return _resultFactory.createResponse(exception);
        } finally {
            totalTimeContext.stop();
        }
    }
}
