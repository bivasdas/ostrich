package com.bazaarvoice.ostrich.perftest.pool.runner;

import com.bazaarvoice.ostrich.perftest.core.runner.ServiceRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.EmbeddedServer;
import com.bazaarvoice.ostrich.perftest.pool.web.TransportData;
import com.bazaarvoice.ostrich.perftest.pool.web.client.HashClient;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.curator.test.TestingServer;

import java.util.List;

public final class ServicePoolRunner extends ServiceRunner {

    private List<EmbeddedServer<?, ?>> _embeddedServers;
    private TestingServer _testingServer;
    private final HashClient _client;
    private final Timer _serviceTimer;
    private final Meter _failureMeter;
    private final Meter _successMeter;

    public ServicePoolRunner(Arguments arguments) throws Exception {
        super(arguments);
        _testingServer = new TestingServer(arguments.getZookeeperPort());
        _embeddedServers = Lists.newLinkedList();
        for (int i = arguments.getServerStartingPort(); i < arguments.getServerStartingPort() + arguments.getNumServers() * 2; i = i + 2) {
            _embeddedServers.add(EmbeddedServer.newServer(i, i + 1));
        }
        for (EmbeddedServer<?, ?> embeddedServer : _embeddedServers) {
            embeddedServer.start();
        }
        _client = new HashClient(_cachingPolicy, null);
        _serviceTimer = MetricsUtility.newTimer(ServicePoolRunner.class, "service-timer");
        _failureMeter = MetricsUtility.newMeter(ServicePoolRunner.class, "failure-meter");
        _successMeter = MetricsUtility.newMeter(ServicePoolRunner.class, "success-meter");
    }

    public List<EmbeddedServer<?, ?>> getEmbeddedServers() {
        return _embeddedServers;
    }

    @Override
    public void close() {
        Utilities.closeParallel(_client);
        for (EmbeddedServer<?, ?> embeddedServer : _embeddedServers) {
            Utilities.closeParallel(embeddedServer);
        }
        Utilities.closeParallel(_testingServer);
    }


    public float getCallRatePercent() {
        return _client.getCallRatePercent();
    }

    @Override
    protected void generateWorker() {
        TransportData result = null;
        Timer.Context clock = _serviceTimer.time();
        try {
            result = _client.call(HashFunction.getRandomHashName(), RandomStringUtils.randomAlphanumeric(_workSize));
        } catch (Exception ignored) {
        } finally {
            clock.stop();
        }

        if(result == null) {
            _failureMeter.mark();
            _client.decreaseCallRate();
        }
        else {
            _successMeter.mark();
            _client.increaseCallRate(result.getUri() != null);
        }
    }
}
