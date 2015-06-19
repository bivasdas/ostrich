package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.registry.zookeeper.ZooKeeperServiceRegistry;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.curator.framework.CuratorFramework;

public class HashService extends Application<HashServiceConfiguration> {

    public static String getServiceName() {
        return "hashService";
    }

    @SuppressWarnings("unchecked")
    public static <C extends Configuration, S extends Application<C>> S newService() {
        return (S) new HashService();
    }

    @Override
    public void initialize(Bootstrap<HashServiceConfiguration> bootstrap) {}

    @Override
    public void run(HashServiceConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(HashServiceResource.class);
        environment.healthChecks().register("hashService", new HashServiceHealthCheck());
        final ServiceEndPoint endPoint = Utilities.buildServiceEndPoint(HashServiceResource.class, configuration, environment);
        final CuratorFramework curator = configuration.getZooKeeperConfiguration().newManagedCurator(environment.lifecycle());
        final ZooKeeperServiceRegistry registry = new ZooKeeperServiceRegistry(curator, environment.metrics());

        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                registry.register(endPoint);
            }

            @Override
            public void stop() throws Exception {
                registry.unregister(endPoint);
            }
        });
    }
}
