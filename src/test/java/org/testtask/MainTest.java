package org.testtask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    private static final int AMOUNT_OF_DATA = 2_000_000;
    // +- 5 seconds to start test and assert results
    private static final int SHOULD_BE_HANDLED_IN = 15;
    private final Collection<String> generatedData = generateData(AMOUNT_OF_DATA);

    @Test
    @Timeout(SHOULD_BE_HANDLED_IN)
    void shouldSupportHighThroughput() throws InterruptedException, ExecutionException, IOException {
        var serverFeature = CompletableFuture.runAsync(() -> {
            try {
                Main.main(new String[0]);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var loadGenerator = new LoadGenerator();
        loadGenerator.generateLoad(generatedData.stream(), 5, 4000);

        // Need to give time for server to remove completed handlers from the thread pool
        // otherwise, it will reject a new connection
        TimeUnit.SECONDS.sleep(2);
        TestHelpers.sendRequest(4000, "terminate");
        serverFeature.get();

        try (var stream = Files.lines(Path.of("numbers.txt"))) {
            var expected = generatedData.stream().distinct().count();
            var actual = stream.count();
            assertEquals(expected, actual);
        }
    }

    private Collection<String> generateData(int amountOfData) {
        int stringLength = 9;
        boolean useLetters = false;
        boolean useNumbers = true;
        return IntStream
                .range(0, amountOfData)
                .parallel()
                .mapToObj((i) -> RandomStringUtils.random(stringLength, useLetters, useNumbers))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.delete(new File("numbers.txt"));
    }
}