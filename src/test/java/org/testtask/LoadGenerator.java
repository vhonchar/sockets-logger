package org.testtask;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class LoadGenerator {

    private final Queue<String> dataQueue = new ConcurrentLinkedQueue<>();
    private int serverPort;

    public void generateLoad(Stream<String> data, int socketsAmount, int serverPort) {
        data.forEach(this.dataQueue::add);
        this.serverPort = serverPort;
        var sockets = new ArrayList<CompletableFuture<Void>>();
        while (socketsAmount > 0) {
            sockets.add(CompletableFuture.runAsync(this::sendDataThroughSocket));
            socketsAmount--;
        }
        CompletableFuture.allOf(sockets.toArray(CompletableFuture[]::new)).join();
        System.out.println("All load generators completed their work");
    }

    private void sendDataThroughSocket() {
        try (var socket = new Socket("localhost", this.serverPort);
             var os = new OutputStreamWriter(socket.getOutputStream())) {
            System.out.println("Starting new load generator on port " + socket.getLocalPort());
            while (!this.dataQueue.isEmpty()) {
                os.write(this.dataQueue.poll() + System.lineSeparator());
            }
            System.out.println("Load generator on port " + socket.getLocalPort() + " completed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
