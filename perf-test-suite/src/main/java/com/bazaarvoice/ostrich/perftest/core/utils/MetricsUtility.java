package com.bazaarvoice.ostrich.perftest.core.utils;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MetricsUtility {

    protected static final MetricsRegistry METRICS_REGISTRY= Metrics.defaultRegistry();
    protected static final Map<MetricName, Metric> ALL_METRICS = METRICS_REGISTRY.allMetrics();

    private MetricsUtility() { }

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
        return (T) ALL_METRICS.get(new MetricName(aClass, name, scope));
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
        return METRICS_REGISTRY.newMeter(new MetricName(aClass, name, scope), name, TimeUnit.SECONDS);
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
        return METRICS_REGISTRY.newTimer(new MetricName(aClass, name, scope), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
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
        return METRICS_REGISTRY.newCounter(new MetricName(aClass, name, scope));
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

    public static <T> Gauge<T> newGauge(Class<?> aClass, String name, String scope, Gauge<T> gauge) {
        return METRICS_REGISTRY.newGauge(new MetricName(aClass, name, scope), gauge);
    }
}
