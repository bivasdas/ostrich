package com.bazaarvoice.ostrich.perftest.core.utils;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.ServiceEndPointBuilder;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Environment;

import javax.validation.Validation;
import javax.ws.rs.core.UriBuilder;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class Utilities {

    private static final ObjectMapper JSON = new MappingJsonFactory().getCodec();
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private Utilities() {
    }

    /**
     * Creates a serviceEndPoint to hash a string with a given hash function
     *
     * @param hashFunctionName to delegate the work
     * @return an appropriate serviceEndPoint for the job
     */
    public static ServiceEndPoint buildServiceEndPoint(String hashFunctionName) {
        return new ServiceEndPointBuilder()
                .withServiceName(hashFunctionName)
                .withId(hashFunctionName)
                .build();
    }

    public static void sleepForSeconds(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException ignored) {
        }
    }

    public static int getRandomInt(int limit) {
        return RANDOM.nextInt(limit);
    }

    public static <T> ServiceEndPoint buildServiceEndPoint(Class<T> aClass, Configuration configuration, Environment environment) throws IOException {
        InetAddress localhost = InetAddress.getLocalHost();
        String host = localhost.getHostName();
        String ip = localhost.getHostAddress();
        int port = getHttpPort(configuration);
        int adminPort = getAdminHttpPort(configuration);

        // The client reads the URLs out of the payload to figure out how to connect to this server.
        URI serviceUri = UriBuilder.fromResource(aClass).scheme("http").host(ip).port(port).build();
        URI adminUri = UriBuilder.fromPath("").scheme("http").host(ip).port(adminPort).build();

        return new ServiceEndPointBuilder()
                .withServiceName(environment.getName())
                .withId(host + ":" + port)
                .withPayload(toPayloadString(serviceUri, adminUri))
                .build();
    }

    public static File createTempConfigFile(String contents, String prefix, String suffix) throws IOException {
        File temporaryConfig = File.createTempFile(prefix, suffix);
        temporaryConfig.deleteOnExit();
        Files.write(temporaryConfig.toPath(), contents.getBytes());
        return temporaryConfig;
    }

    public static String createDWConfigYAML(int port, int adminPort) {
        return "server:\n" +
                "  applicationConnectors:\n" +
                "    - type: http\n" +
                "      port: " + port + "\n" +
                "  adminConnectors:\n" +
                "    - type: http\n" +
                "      port: " + adminPort + "\n";
    }

    public static <C extends Configuration, S extends Application<C>> C getConfiguration(S service, File yamlConfigFile)
            throws Exception {
        ConfigurationFactory<C> factory = new ConfigurationFactory<>(service.getConfigurationClass(),
                Validation.buildDefaultValidatorFactory().getValidator(), Jackson.newObjectMapper(), service.getName());
        if (yamlConfigFile == null) {
            return factory.build();
        } else {
            return factory.build(yamlConfigFile);
        }
    }

    public static <C extends Configuration, S extends Application<C>> C getConfiguration(Class<C> cClass, File yamlConfigFile, String serviceName)
            throws Exception {
        ConfigurationFactory<C> factory = new ConfigurationFactory<>(cClass, Validation.buildDefaultValidatorFactory().getValidator(),
                Jackson.newObjectMapper(), serviceName);
        if (yamlConfigFile == null) {
            return factory.build();
        } else {
            return factory.build(yamlConfigFile);
        }
    }

    public static URI getAdminUriFromPayload(String payload) {
        return getUriFromPayload(payload, "adminUrl");
    }

    public static URI getServiceUriFromPayload(String payload) {
        return getUriFromPayload(payload, "url");
    }

    private static URI getUriFromPayload(String payload, String fieldName) {
        try {
            Map<?, ?> map = JSON.readValue(payload, Map.class);
            return URI.create((String) checkNotNull(map.get(fieldName), fieldName));
        } catch (IOException e) {
            throw new AssertionError(e); // Shouldn't get IO errors reading from a string
        }
    }

    public static String toPayloadString(URI serviceUri, URI adminUri) {
        try {
            Map<String, URI> payload = ImmutableMap.of("url", serviceUri, "adminUrl", adminUri);
            return JSON.writeValueAsString(payload);
        } catch (IOException e) {
            throw new AssertionError(e);  // Shouldn't get IO errors writing to a string
        }
    }

    public static long currentTimeSeconds() {
        return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    // Snapshot returns times in NS, this converts them to ms as we need
    public static double nsToMs(double ns) {
        return ns / 1000000;
    }

    public static String buildString(String metricName, long count, double minuteRate, double meanRate) {
        return String.format("%-40s %10s: %20d %20s: %12.2f %15s: %12.2f",
                metricName, "count", count, "1-min-rate", minuteRate, "mean-rate", meanRate);
    }

    public static String buildString(String metricName, double meanTime, double minTime, double maxTime) {
        return String.format("%-40s %10s: %23.2f %17s: %12.2f %15s: %12.2f",
                metricName + " Time", "mean", meanTime, "min", minTime, "max", maxTime);
    }

    public static String buildString(String metricName, long count) {
        return String.format("%-40s %10s: %20d", metricName, "count", count);
    }

    public static String buildString(String metricName, Object value) {
        return String.format("%-40s %10s: %20s", metricName, "value", value);
    }

    public static void printMetric(String metricName, Metric metric) {
        if (metric instanceof Meter) {
            Meter metered = (Meter) metric;
            System.out.println(buildString(metricName, metered.getCount(), metered.getOneMinuteRate(), metered.getMeanRate()));
        } else if (metric instanceof Timer) {
            Timer timer = (Timer) metric;
            Snapshot snapshot = timer.getSnapshot();
            System.out.println(buildString(metricName, timer.getCount(), timer.getOneMinuteRate(), timer.getMeanRate()));
            System.out.println(buildString(metricName, snapshot.getMean(), snapshot.getMin(), snapshot.getMax()));
        } else if (metric instanceof Counter) {
            System.out.println(buildString(metricName, ((Counter) metric).getCount()));
        } else if (metric instanceof Gauge<?>) {
            System.out.println(buildString(metricName, ((Gauge) metric).getValue()));
        } else {
            System.out.println(buildString(metricName, metric));
        }
    }

    @SuppressWarnings ("deprecation")
    public static void closeParallel(final Closeable item) {
        new Thread() {
            @Override
            public void run() {
                closeQuietly(item);
                System.out.print(".");
            }
        }.start();
    }

    public static void closeQuietly(Closeable item) {
        try {
            item.close();
        }
        catch(IOException ignored) {}
    }

    public static int getHttpPort(Configuration config) {
        ServerFactory serverFactory = config.getServerFactory();
        if (!(serverFactory instanceof DefaultServerFactory)) {
            throw new IllegalStateException("Server factory is not an instance of DefaultServerFactory");
        }

        List<ConnectorFactory> connectors = ((DefaultServerFactory) serverFactory).getApplicationConnectors();
        for (ConnectorFactory connector : connectors) {
            if (connector instanceof HttpConnectorFactory) {
                return ((HttpConnectorFactory) connector).getPort();
            }
        }

        throw new IllegalStateException("Unable to determine HTTP port");
    }

    public static int getAdminHttpPort(Configuration config) {
        ServerFactory serverFactory = config.getServerFactory();
        if (!(serverFactory instanceof DefaultServerFactory)) {
            throw new IllegalStateException("Server factory is not an instance of DefaultServerFactory");
        }

        List<ConnectorFactory> connectors = ((DefaultServerFactory) serverFactory).getAdminConnectors();
        for (ConnectorFactory connector : connectors) {
            if (connector instanceof HttpConnectorFactory) {
                return ((HttpConnectorFactory) connector).getPort();
            }
        }

        throw new IllegalStateException("Unable to determine admin HTTP port");
    }

}
