package com.bazaarvoice.ostrich.perftest.pool.web;

import com.bazaarvoice.ostrich.perftest.core.utils.MetricsUtility;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.perftest.pool.web.service.HashService;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Server;

import javax.validation.Validation;
import java.io.Closeable;
import java.io.File;

/**
 * Based on the code listed here : https://github.com/codahale/dropwizard/issues/120.
 *
 * This basically emulates what happens when you call a Dropwizard's static main method, with the goal being
 *  to get a handle to the Jetty server, so that it can be started and stopped in the context of the test.
 *
 */
public class EmbeddedServer<C extends Configuration, S extends Application<C>> implements Closeable {

    @SuppressWarnings("unchecked")
    public static <C extends Configuration, S extends Application<C>> EmbeddedServer<C, S> newServer(int port, int adminPort) throws Exception {
        File configFile = Utilities.createTempConfigFile(Utilities.createDWConfigYAML(port, adminPort), "dropwizard", ".yaml");
        return newServer((S) HashService.newService(), configFile);
    }

    public static <C extends Configuration, S extends Application<C>> EmbeddedServer<C, S> newServer(S service, File yamlConfigFile) throws Exception {
        return newServer(service, Utilities.getConfiguration(service, yamlConfigFile));
    }

    public static <C extends Configuration, S extends Application<C>> EmbeddedServer<C, S> newServer(S service, C configuration) throws Exception {
        String environmentName = HashService.getServiceName();
        Environment environment = new Environment(environmentName, Jackson.newObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator(),
                MetricsUtility.getMetricsRegistry(), Thread.currentThread().getContextClassLoader());
        service.run(configuration, environment);
        Server server = new DefaultServerFactory().build(environment);
        return new EmbeddedServer<>(server, configuration);
    }

    private final Server _server;
    private final int _port;

    private EmbeddedServer(Server server, C configuration) {
        _server = server;
        _port = Utilities.getHttpPort(configuration);
    }

    public int getPort() {
        return _port;
    }

    public void start() throws Exception {
        _server.start();
    }

    @Override
    public void close() {
        try {
            _server.stop();
        } catch (Exception ignored) {}
        try {
            _server.join();
        } catch (Exception ignored) {}
        try{
            _server.destroy();
        } catch (Exception ignored) {}
    }
}
