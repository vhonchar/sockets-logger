package org.newrelic;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class ServerTest {

    private static final int ANY_OPEN_PORT = 0;

    private Server server;

    @BeforeEach
    void setUp() {
        this.server = new Server(3);
    }

    @Test
    void shouldAcceptOnlyConfiguredAmountOfClients() throws InterruptedException, ExecutionException, IOException {
        var receivedSockets = new CountDownLatch(3);
        this.server.start(ANY_OPEN_PORT, (input, output) -> {
            receivedSockets.countDown();
            output.write("response");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // start 3 sockets
        Client.ping(this.server.getPort());
        Client.ping(this.server.getPort());
        Client.ping(this.server.getPort());
        // await until all three sockets reach to the server
        receivedSockets.await();

        // try to connect one more time, expect error
        assertEquals("Too many clients", Client.ping(this.server.getPort()).get());
    }

    @Test
    void shouldHandleSocketRequestAndReturnResponseFromServer() throws ExecutionException, InterruptedException, IOException {
        this.server.start(ANY_OPEN_PORT, (input, output) -> {
            var receivedText = input.readLine();
            output.write("Server received " + receivedText);
        });

        var actualResponse = Client.sendRequest(this.server.getPort(), "request text").get();

        assertEquals("Server received request text", actualResponse);
    }

    @AfterEach
    void tearDown() {
        IOUtils.closeQuietly(this.server);
    }
}