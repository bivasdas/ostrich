package com.bazaarvoice.ostrich.perftest.pool.runner;

import com.bazaarvoice.ostrich.discovery.zookeeper.ZooKeeperHostDiscovery;
import com.bazaarvoice.ostrich.perftest.core.runner.ChaosRunner;
import com.bazaarvoice.ostrich.perftest.core.runner.LoadRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.client.HashClient;
import com.bazaarvoice.ostrich.perftest.pool.web.client.HashServiceClientServiceFactory;
import com.bazaarvoice.ostrich.perftest.pool.web.service.HashService;
import com.bazaarvoice.ostrich.registry.zookeeper.ZooKeeperServiceRegistry;

public class PoolLoadRunner extends LoadRunner {

    private final ServicePoolRunner _servicePoolRunner;

    public PoolLoadRunner(Arguments arguments) throws Exception {
        super(arguments);

        _servicePoolRunner = new ServicePoolRunner(arguments);
        PoolChaosRunner poolChaosRunner = new PoolChaosRunner(arguments, _servicePoolRunner.getEmbeddedServers());

        _workers.addAll(_servicePoolRunner.generateWorkers());
        _workers.addAll(poolChaosRunner.generateChaosWorkers());

        String serviceName = HashService.getServiceName();

        _allMetrics.put("Chaos Created", MetricsUtility.get(ChaosRunner.class, "Chaos"));
        _allMetrics.put("Stable Created", MetricsUtility.get(ChaosRunner.class, "Stable"));

        _allMetrics.put("Client Total Called", MetricsUtility.get(HashClient.class, "total-client-called"));
        _allMetrics.put("Client Actual Called", MetricsUtility.get(HashClient.class, "actual-client-called"));
        _allMetrics.put("Client Noop Called", MetricsUtility.get(HashClient.class, "noop-client-called"));
        _allMetrics.put("Client Total", MetricsUtility.get(HashClient.class, "execution-timer"));

        _allMetrics.put("Service Factory Created", MetricsUtility.get(HashServiceClientServiceFactory.class, "Service-Created"));
        _allMetrics.put("Service Factory Destroyed", MetricsUtility.get(HashServiceClientServiceFactory.class, "Service-Destroyed"));

        _allMetrics.put("Runner Service", MetricsUtility.get(ServicePoolRunner.class, "service-timer"));
        _allMetrics.put("Runner Service Success", MetricsUtility.get(ServicePoolRunner.class, "success-meter"));
        _allMetrics.put("Runner Service Failure", MetricsUtility.get(ServicePoolRunner.class, "failure-meter"));

        _allMetrics.put("ZK Registry Registered EndPoints", MetricsUtility.newCounter(ZooKeeperServiceRegistry.class, "num-registered-end-points", serviceName));

        _allMetrics.put("ZK Discovery Removed EndPoints", MetricsUtility.get(ZooKeeperHostDiscovery.class, "num-zookeeper-removes", serviceName));
        _allMetrics.put("ZK Discovery Added EndPoints", MetricsUtility.get(ZooKeeperHostDiscovery.class, "num-zookeeper-adds", serviceName));
        _allMetrics.put("ZK Discovery EndPoint Counts", MetricsUtility.get(ZooKeeperHostDiscovery.class, "num-end-points", serviceName));

        _allMetrics.put("Pool Execution", MetricsUtility.get("com.bazaarvoice.ostrich.pool.ServicePool", "callback-execution-time", serviceName));
        _allMetrics.put("Pool Execution Success", MetricsUtility.get("com.bazaarvoice.ostrich.pool.ServicePool", "num-execute-successes", serviceName));
        _allMetrics.put("Pool Execution Failure", MetricsUtility.get("com.bazaarvoice.ostrich.pool.ServicePool", "num-execute-attempt-failures", serviceName));
        _allMetrics.put("Pool Valid EndPoints", MetricsUtility.get("com.bazaarvoice.ostrich.pool.ServicePool", "num-valid-end-points", serviceName));
        _allMetrics.put("Pool Bad EndPoints", MetricsUtility.get("com.bazaarvoice.ostrich.pool.ServicePool", "num-bad-end-points", serviceName));

        if (arguments.useMultiThreadedCache()) {
            _allMetrics.put("Cache Eviction", MetricsUtility.get("com.bazaarvoice.ostrich.pool.MultiThreadedClientServiceCache", "eviction-time", serviceName));
            _allMetrics.put("Cache Register", MetricsUtility.get("com.bazaarvoice.ostrich.pool.MultiThreadedClientServiceCache", "register-time", serviceName));
            _allMetrics.put("Cache Service Counter", MetricsUtility.newCounter("com.bazaarvoice.ostrich.pool.MultiThreadedClientServiceCache", "service-counter", serviceName));
        } else {
            _allMetrics.put("Cache Load", MetricsUtility.get("com.bazaarvoice.ostrich.pool.SingleThreadedClientServiceCache", "load-time", serviceName));
        }

        for (Thread thread : _workers) {
            thread.start();
        }
    }

    @Override
    public void close() {
        _servicePoolRunner.close();
    }

    @Override
    public void printHeaders() {
        long currentRuntime = Utilities.currentTimeSeconds() - _startTime;
        System.out.println(String.format(_headerDateTime, currentRuntime));
        if (_arguments.useMultiThreadedCache()) {
            System.out.println(_headerNewCache +
                    String.format(", request-rate: %.4f%%", _servicePoolRunner.getCallRatePercent()));
        } else {
            System.out.println(_headerOldCache +
                    String.format(", request-rate: %.4f%%", _servicePoolRunner.getCallRatePercent()));
        }
    }
}
