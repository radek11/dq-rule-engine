package io.github.radek11.dq.example.http;

import com.sun.net.httpserver.HttpServer;
import io.github.radek11.dq.RuleEngine;
import io.github.radek11.dq.RunOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Example host: a small HTTP API that validates a JSON array of records with the section 5 rules.
 *
 * <p>Usage: {@code ./gradlew :example-http:run [--args=PORT]} (default 8080), then
 * {@code curl --data-binary @example-http/fixture/records.json http://localhost:8080/validate}.
 */
public final class Main {

    /**
     * Starts the server on localhost.
     *
     * @param args optional port, default 8080
     * @throws IOException when the port cannot be bound
     */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = start(port);
        System.out.println("Listening on http://localhost:" + server.getAddress().getPort() + "/validate");
    }

    /**
     * One engine for all requests: it holds no run state (DESIGN §2); the catalog is immutable and
     * every request gets its own sink. Each request runs on its own virtual thread, so a long
     * upload does not hold up the others.
     */
    static HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        BatchValidation validation =
                new BatchValidation(new RuleEngine(FixtureRules.catalog()), RunOptions.defaults());
        server.createContext("/validate", new ValidateHandler(validation));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        return server;
    }

    private Main() {
    }
}
