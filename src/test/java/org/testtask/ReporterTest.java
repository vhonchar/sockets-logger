package org.testtask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReporterTest {

    private Reporter reporter;
    private MetricsService metrics;

    @BeforeEach
    public void setup() {
        this.metrics = new MetricsService();
        this.reporter = new Reporter(metrics);
    }

    @Test
    void shouldReportZeroInitialData() {
        assertEquals("Received 0 unique numbers, 0 duplicates. Unique total: 0", this.reporter.runReport());
    }

    @Test
    void shouldReportTotalOnFirstRun() {
        this.metrics.unique().getAndAdd(500);
        this.metrics.duplicates().getAndAdd(1_000_000);
        assertEquals("Received 500 unique numbers, 1000000 duplicates. Unique total: 500", this.reporter.runReport());
    }

    @Test
    void shouldReportDeltaFromLastRun() {
        this.metrics.unique().getAndAdd(500);
        this.metrics.duplicates().getAndAdd(1_000_000);
        assertEquals("Received 500 unique numbers, 1000000 duplicates. Unique total: 500", this.reporter.runReport());

        this.metrics.unique().getAndAdd(300);
        this.metrics.duplicates().getAndAdd(5000);
        assertEquals("Received 300 unique numbers, 5000 duplicates. Unique total: 800", this.reporter.runReport());
    }
}