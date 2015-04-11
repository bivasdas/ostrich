package com.bazaarvoice.ostrich.perftest.core.utils;

import com.bazaarvoice.ostrich.ServiceEndPoint;
import com.bazaarvoice.ostrich.ServiceEndPointBuilder;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.json.ObjectMapperFactory;
import com.yammer.dropwizard.validation.Validator;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.Timer;

import javax.ws.rs.core.UriBuilder;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
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
        int port = configuration.getHttpConfiguration().getPort();
        int adminPort = configuration.getHttpConfiguration().getAdminPort();

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
        return "zooKeeper:\n" +
                "  connectString: localhost:2181\n" +
                "\n" +
                "http:\n" +
                "  port: " + port + "\n" +
                "  adminPort: " + adminPort + "\n" +
                "  requestLog:\n" +
                "    console:\n" +
                "      enabled: " + Boolean.FALSE;
    }

    public static <C extends Configuration, S extends Service<C>> C getConfiguration(S service, File yamlConfigFile)
            throws Exception {
        ConfigurationFactory<C> factory =
                ConfigurationFactory.forClass(service.getConfigurationClass(), new Validator(), new ObjectMapperFactory());
        if (yamlConfigFile == null) {
            return factory.build();
        } else {
            return factory.build(yamlConfigFile);
        }
    }

    public static <C extends Configuration, S extends Service<C>> C getConfiguration(Class<C> cClass, File yamlConfigFile)
            throws Exception {
        ConfigurationFactory<C> factory =
                ConfigurationFactory.forClass(cClass, new Validator(), new ObjectMapperFactory());
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
            System.out.println(buildString(metricName, metered.count(), metered.oneMinuteRate(), metered.meanRate()));
        } else if (metric instanceof Timer) {
            Timer timer = (Timer) metric;
            System.out.println(buildString(metricName, timer.count(), timer.oneMinuteRate(), timer.meanRate()));
            System.out.println(buildString(metricName, timer.mean(), timer.min(), timer.max()));
        } else if (metric instanceof Counter) {
            System.out.println(buildString(metricName, ((Counter) metric).count()));
        } else if (metric instanceof Gauge<?>) {
            System.out.println(buildString(metricName, ((Gauge) metric).value()));
        } else {
            System.out.println(buildString(metricName, metric));
        }
    }

    @SuppressWarnings ("deprecation")
    public static void closeParallel(final Closeable item) {
        new Thread() {
            @Override
            public void run() {
                Closeables.closeQuietly(item);
                System.out.print(".");
            }
        }.start();
    }


}
