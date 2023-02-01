package org.newrelic;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server implements Closeable {

    private final ThreadPoolExecutor executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server(int concurrentExecutorsAmount) {
        // Up to configurable amount of executors, handling requests in parallel.
        // No queue, since we need to accept only N clients, not more.
        // All new clients will be rejected
        this.executor = new ThreadPoolExecutor(
                concurrentExecutorsAmount,
                concurrentExecutorsAmount,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
        );
    }

    /**
     * Start accepting incoming requests in a separated thread,
     * so we could stop the server from this thread.
     */
    public void start(int port, SocketCallback socketCallback) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.running = true;
        System.out.println("Started server on port " + this.serverSocket.getLocalPort());
        new Thread(() -> this.acceptNewSocketRequests(socketCallback)).start();
    }

    private void acceptNewSocketRequests(SocketCallback socketCallback) {
        while (this.running) {
            try {
                var client = this.serverSocket.accept();
                try {
                    this.executor.execute(() -> this.handleClientSocket(client, socketCallback));
                } catch (RejectedExecutionException e) {
                    this.reject(client);
                }
            } catch (IOException e) {
                if (!this.serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Server is shutting down");
    }

    private void reject(Socket client) {
        System.out.println("Rejected incoming request from port because of server's pool limitation");
        if (client != null) {
            try (var output = new OutputStreamWriter(client.getOutputStream())) {
                output.write("Too many clients");
                output.flush();
                client.close();
            } catch (IOException ex) {
                IOUtils.closeQuietly(client);
            }
        }
    }

    private void handleClientSocket(Socket client, SocketCallback socketCallback) {
        BufferedReader input = null;
        BufferedWriter output = null;
        try {
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            System.out.println("Responding a request from socket " + client.getPort());

            socketCallback.accept(input, output);

            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    @Override
    public void close() throws IOException {
        this.running = false;
        this.serverSocket.close();
        this.executor.shutdown();
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    public boolean isRunning() {
        return this.running;
    }

    // same as BiConsumer, just can throw IOException
    @FunctionalInterface
    public static interface SocketCallback {
        void accept(BufferedReader input, BufferedWriter output) throws IOException;
    }
}
