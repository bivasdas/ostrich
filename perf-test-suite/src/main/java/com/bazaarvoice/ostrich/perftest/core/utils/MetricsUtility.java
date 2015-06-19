package com.bazaarvoice.ostrich.perftest.core.utils;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Map;

public final class MetricsUtility {

    private static final MetricRegistry METRICS_REGISTRY = new MetricRegistry();
    private static final Map<String, Metric> ALL_METRICS = METRICS_REGISTRY.getMetrics();

    private MetricsUtility() { }

    public static MetricRegistry getMetricsRegistry() {
        return METRICS_REGISTRY;
    }

    @SuppressWarnings ("unchecked")
    public static <T extends Metric> T get(String fqdnClassName, String name) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return get(aClass, name, null);
    }

    @SuppressWarnings ("unchecked")
    public static <T extends Metric> T get(Class<?> aClass, String name) {
        return get(aClass, name, null);
    }

    @SuppressWarnings ("unchecked")
    public static <T extends Metric> T get(String fqdnClassName, String name, String scope) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return get(aClass, name, scope);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Metric> T get(Class<?> aClass, String name, String scope) {
        return (T) ALL_METRICS.get(MetricRegistry.name(aClass, name, scope));
    }

    // meters

    public static Meter newMeter(String fqdnClassName, String name) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newMeter(aClass, name, null);
    }

    public static Meter newMeter(Class<?> aClass, String name) {
        return newMeter(aClass, name, null);
    }

    public static Meter newMeter(String fqdnClassName, String name, String scope) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newMeter(aClass, name, scope);
    }

    public static Meter newMeter(Class<?> aClass, String name, String scope) {
        return METRICS_REGISTRY.meter(MetricRegistry.name(aClass, name, scope));
    }

    // timers

    public static Timer newTimer(String fqdnClassName, String name) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newTimer(aClass, name, null);
    }

    public static Timer newTimer(Class<?> aClass, String name) {
        return newTimer(aClass, name, null);
    }

    public static Timer newTimer(String fqdnClassName, String name, String scope) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newTimer(aClass, name, scope);
    }

    public static Timer newTimer(Class<?> aClass, String name, String scope) {
        return METRICS_REGISTRY.timer(MetricRegistry.name(aClass, name, scope));
    }

    // counters

    public static Counter newCounter(String fqdnClassName, String name) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newCounter(aClass, name, null);
    }

    public static Counter newCounter(Class<?> aClass, String name) {
        return newCounter(aClass, name, null);
    }

    public static Counter newCounter(String fqdnClassName, String name, String scope) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newCounter(aClass, name, scope);
    }

    public static Counter newCounter(Class<?> aClass, String name, String scope) {
        return METRICS_REGISTRY.counter(MetricRegistry.name(aClass, name, scope));
    }

    // gauges

    public static <T> Gauge<T> newGauge(String fqdnClassName, String name, Gauge<T> gauge) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newGauge(aClass, name, null, gauge);
    }

    public static <T> Gauge<T> newGauge(Class<?> aClass, String name, Gauge<T> gauge) {
        return newGauge(aClass, name, null, gauge);
    }

    public static <T> Gauge<T> newGauge(String fqdnClassName, String name, String scope, Gauge<T> gauge) throws ClassNotFoundException {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fqdnClassName);
        return newGauge(aClass, name, scope, gauge);
    }

    @SuppressWarnings("unchecked")
    public static <T> Gauge<T> newGauge(Class<?> aClass, String name, String scope, Gauge<T> gauge) {
        String fullName = MetricRegistry.name(aClass, name, scope);
        Gauge<T> metric = (Gauge<T>) METRICS_REGISTRY.getMetrics().get(fullName);
        if (metric == null) {
            metric = METRICS_REGISTRY.register(fullName, gauge);
        }
        return metric;
    }
}
