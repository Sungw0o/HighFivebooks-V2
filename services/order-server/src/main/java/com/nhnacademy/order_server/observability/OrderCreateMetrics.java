package com.nhnacademy.order_server.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class OrderCreateMetrics {

    public static final String METRIC_NAME = "order.create.segment";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public OrderCreateMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> T record(String segment, Supplier<T> action) {
        return timer(segment).record(action);
    }

    public void record(String segment, Runnable action) {
        timer(segment).record(action);
    }

    private Timer timer(String segment) {
        return timers.computeIfAbsent(segment, key -> Timer.builder(METRIC_NAME)
                .description("Order creation critical-path segment latency")
                .tag("segment", key)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }
}
