package org.testtask;

import static java.lang.String.format;

public class Reporter {

    private static final String REPORT_FORMAT = "Received %s unique numbers, %s duplicates. Unique total: %s";
    private final MetricsService metrics;
    private int lastUnique = 0;
    private int lastDuplicates = 0;

    public Reporter(MetricsService metrics) {
        this.metrics = metrics;
    }

    public String runReport() {
        var totalUnique = this.metrics.unique().get();
        var totalDuplicates = this.metrics.duplicates().get();

        var report = format(REPORT_FORMAT,
                totalUnique - this.lastUnique,
                totalDuplicates - this.lastDuplicates,
                totalUnique
        );

        this.lastDuplicates = totalDuplicates;
        this.lastUnique = totalUnique;

        return report;
    }
}
