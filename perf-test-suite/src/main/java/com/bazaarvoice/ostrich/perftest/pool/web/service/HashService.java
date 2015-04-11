package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.perftest.core.utils.Utilities;
import com.bazaarvoice.ostrich.registry.zookeeper.ZooKeeperServiceRegistry;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import org.apache.curator.framework.CuratorFramework;

public class HashService extends Service<HashServiceConfiguration> {

    public static String getServiceName() {
        return "hashService";
    }

    @SuppressWarnings("unchecked")
    public static <C extends Configuration, S extends Service<C>> S newService() {
        return (S) new HashService();
    }

    @Override
    public void initialize(Bootstrap<HashServiceConfiguration> bootstrap) {
        bootstrap.setName(getServiceName());
    }

    @Override
    public void run(HashServiceConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(HashServiceResource.class);
        environment.addHealthCheck(new HashServiceHealthCheck());
        final ServiceEndPoint endPoint = Utilities.buildServiceEndPoint(HashServiceResource.class, configuration, environment);
        final CuratorFramework curator = configuration.getZooKeeperConfiguration().newManagedCurator(environment);
        final ZooKeeperServiceRegistry registry = new ZooKeeperServiceRegistry(curator);

        environment.manage(new Managed() {
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
