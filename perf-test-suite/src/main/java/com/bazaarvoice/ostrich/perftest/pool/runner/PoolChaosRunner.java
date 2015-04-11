package com.bazaarvoice.ostrich.perftest.pool.runner;

import com.bazaarvoice.ostrich.perftest.core.runner.ChaosRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.EmbeddedServer;
import com.google.common.collect.Queues;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class PoolChaosRunner extends ChaosRunner {
    private final List<EmbeddedServer<?, ?>> _embeddedServers;
    private final Queue<Integer> availablePorts = Queues.newConcurrentLinkedQueue();

    public PoolChaosRunner(Arguments arguments, List<EmbeddedServer<?, ?>> embeddedServers) {
        super(arguments);
        _embeddedServers = Collections.synchronizedList(embeddedServers);
        int startingReservePort = arguments.getServerStartingPort() + arguments.getNumServers() * 2;
        for (int i = startingReservePort; i < startingReservePort + arguments.getNumServers() * 8; i = i + 2) {
            availablePorts.add(i);
        }
    }

    @Override
    protected void doStable() throws Exception {
        int newPort = availablePorts.remove();
        EmbeddedServer server = EmbeddedServer.newServer(newPort, newPort + 1);
        server.start();
        _embeddedServers.add(server);
    }

    @Override
    protected void doChaos() throws Exception {
        EmbeddedServer embeddedServer = getRandom();
        availablePorts.add(embeddedServer.getPort());
        embeddedServer.close();
    }

    private EmbeddedServer<?, ?> getRandom() {
        return _embeddedServers.remove(Utilities.getRandomInt(_embeddedServers.size()));
    }
}
