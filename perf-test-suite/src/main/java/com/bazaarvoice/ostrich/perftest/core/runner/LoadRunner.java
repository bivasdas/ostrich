package com.bazaarvoice.ostrich.perftest.core.runner;

import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.codahale.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.Closeable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class LoadRunner implements Closeable {

    protected final long _startTime;
    protected final long _totalRuntime;
    protected final List<Thread> _workers;
    protected final int _reportingIntervalSeconds;
    protected final Arguments _arguments;
    protected final String _headerDateTime;
    protected final String _headerOldCache;
    protected final String _headerNewCache;
    protected final String _divider;
    protected final Map<String, Metric> _allMetrics;

    protected boolean _stopAll = false;

    protected LoadRunner(Arguments arguments) {
        _startTime = Utilities.currentTimeSeconds();
        _totalRuntime = arguments.getRunTimeSecond();
        _arguments = arguments;
        _reportingIntervalSeconds = arguments.getReportingIntervalSeconds();

        _headerDateTime = String.format("%s Running %s: %%d seconds of %s", new Date(), _arguments.getTestType(), _arguments.getRunTimeSecond());
        _headerNewCache = String.format("threads: %d, work size: %d, singleton-mode: %s, chaos-worker: %d, chaos-interval: %d, num-servers: %d",
                _arguments.getThreadSize(), _arguments.getWorkSize(), _arguments.useMultiThreadedCache(), _arguments.getChaosWorkers(),
                _arguments.getChaosInterval(), _arguments.getNumServers());

        _headerOldCache = String.format("threads: %d, work size: %d, idle time: %d, max instance: %d, exhaust action: %s, chaos-worker: %d, chaos-interval: %d",
                _arguments.getThreadSize(), _arguments.getWorkSize(), _arguments.getIdleTimeSecond(), _arguments.getMaxInstance(),
                _arguments.getExhaustionAction().name(), _arguments.getChaosWorkers(), _arguments.getChaosInterval());

        _divider = "...............................................................................................................................................";
        _workers = Lists.newLinkedList();
        _allMetrics = Maps.newLinkedHashMap();
    }

    public boolean shouldContinue() {

        long spent = Utilities.currentTimeSeconds() - _startTime;
        boolean shouldContinue = true;

        if (spent >= _totalRuntime) {
            shouldContinue = false;
        }

        if (!shouldContinue) {
            for (Thread t : _workers) {
                t.interrupt();
            }
        }
        return !_stopAll && shouldContinue;
    }

    public void runTest() {
        printHeaders();
        do {
            printLog();
        } while (shouldContinue());
    }

    public Thread getShutdownHook() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                _stopAll = true;
                System.out.print(" Exiting.");
                for(Thread t: _workers) {
                    t.interrupt();
                }
                Utilities.closeParallel(LoadRunner.this);
            }
        });
    }

    public void printLog() {
        System.out.print("\u001b[2J");
        printHeaders();
        for (Map.Entry<String, Metric> entry : _allMetrics.entrySet()) {
            System.out.println(_divider);
            Utilities.printMetric(entry.getKey(), entry.getValue());
        }
        System.out.flush();
        Utilities.sleepForSeconds(_reportingIntervalSeconds);
    }

    public abstract void printHeaders();
}
