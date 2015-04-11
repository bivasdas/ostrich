package com.bazaarvoice.ostrich.perftest.core.runner;

import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.google.common.collect.ImmutableList;
import com.yammer.metrics.core.Meter;

import java.util.List;

public abstract class ChaosRunner {

    protected final int _chaosWorkers;
    protected final Meter _chaosMeter;
    protected final Meter _stableMeter;
    protected final int _chaosInterval;

    protected ChaosRunner(Arguments arguments) {
        _chaosWorkers = arguments.getChaosWorkers();
        _chaosMeter = MetricsUtility.newMeter(ChaosRunner.class, "Chaos");
        _stableMeter = MetricsUtility.newMeter(ChaosRunner.class, "Stable");
        _chaosInterval = arguments.getChaosInterval();
    }

    public List<Thread> generateChaosWorkers() {
        ImmutableList.Builder<Thread> chaosWorkersBuilder = ImmutableList.builder();
        for (int i = 0; i < _chaosWorkers; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        try {
                            int sleepTime = Utilities.getRandomInt(_chaosInterval);
                            Utilities.sleepForSeconds(sleepTime);
                            doChaos();
                            _chaosMeter.mark();
                            Utilities.sleepForSeconds(_chaosInterval - sleepTime);
                        }
                        catch (Exception ignored) {}
                    }
                }
            };
            chaosWorkersBuilder.add(new Thread(runnable));
            runnable = new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        try {
                            int sleepTime = Utilities.getRandomInt(_chaosInterval);
                            Utilities.sleepForSeconds(sleepTime);
                            doStable();
                            _stableMeter.mark();
                            Utilities.sleepForSeconds(_chaosInterval - sleepTime);
                        }
                        catch (Exception ignored) {}
                    }
                }
            };
            chaosWorkersBuilder.add(new Thread(runnable));
        }
        return chaosWorkersBuilder.build();
    }

    protected abstract void doChaos() throws Exception;

    protected abstract void doStable() throws Exception;

}
