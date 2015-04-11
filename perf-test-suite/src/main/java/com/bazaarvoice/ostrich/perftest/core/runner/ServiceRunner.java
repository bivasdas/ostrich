package com.bazaarvoice.ostrich.perftest.core.runner;

import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicy;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicyBuilder;
import com.google.common.collect.ImmutableList;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class ServiceRunner implements Closeable {

    protected final int _threadSize;
    protected final int _workSize;
    protected final ServiceCachingPolicy _cachingPolicy;


    protected ServiceRunner(Arguments arguments) {
        _workSize = arguments.getWorkSize();
        _threadSize = arguments.getThreadSize();

        if (arguments.useMultiThreadedCache()) {
            _cachingPolicy = ServiceCachingPolicyBuilder.getMultiThreadedClientPolicy();
        } else {
            _cachingPolicy = new ServiceCachingPolicyBuilder()
                    .withCacheExhaustionAction(arguments.getExhaustionAction())
                    .withMaxNumServiceInstancesPerEndPoint(arguments.getMaxInstance())
                    .withMaxServiceInstanceIdleTime(arguments.getIdleTimeSecond(), TimeUnit.SECONDS)
                    .build();
        }
    }

    public List<Thread> generateWorkers() {
        ImmutableList.Builder<Thread> threadListBuilder = ImmutableList.builder();
        for (int i = 0; i < _threadSize; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        generateWorker();
                    }
                }
            };
            Thread thread = new Thread(runnable);
            threadListBuilder.add(thread);
        }
        return threadListBuilder.build();
    }

    protected abstract void generateWorker();
}
