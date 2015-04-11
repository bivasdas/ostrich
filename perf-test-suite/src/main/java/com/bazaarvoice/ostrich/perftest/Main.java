package com.bazaarvoice.ostrich.perftest;

import com.bazaarvoice.ostrich.perftest.cache.runner.CacheLoadRunner;
import com.bazaarvoice.ostrich.perftest.core.runner.LoadRunner;
import com.bazaarvoice.ostrich.perftest.core.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.pool.runner.PoolLoadRunner;

public class Main {

    // disable all logging
    static {
        java.util.logging.LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        globalLogger.setLevel(java.util.logging.Level.OFF);

        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().addAppender(new org.apache.log4j.varia.NullAppender());
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
        root.setLevel(ch.qos.logback.classic.Level.OFF);
    }

    public static void main(String args[]) throws Exception {

        Arguments arguments = new Arguments(args);
        LoadRunner loadRunner;

        if(arguments.isPoolTest()) {
            loadRunner = new PoolLoadRunner(arguments);
        }
        else if(arguments.isCacheTest()) {
            loadRunner = new CacheLoadRunner(arguments);
        }
        else {
            throw new Exception("Initialization error");
        }
        Runtime.getRuntime().addShutdownHook(loadRunner.getShutdownHook());
        loadRunner.runTest();
    }
}
