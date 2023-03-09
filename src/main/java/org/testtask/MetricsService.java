package org.testtask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsService {

    private Map<String, AtomicInteger> metrics = new ConcurrentHashMap<>();

    public AtomicInteger getMetric(String name) {
        if (!this.metrics.containsKey(name)) {
            this.metrics.put(name, new AtomicInteger(0));
        }
        return this.metrics.get(name);
    }

    //just alias
    public AtomicInteger unique() {
        return getMetric("unique");
    }

    //just alias
    public AtomicInteger duplicates() {
        return getMetric("duplicates");
    }
}
