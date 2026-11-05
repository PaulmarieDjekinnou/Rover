package fr.paulm.projects.rover.ground;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.paulm.projects.rover.telemetry.TelemetryJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

// The web page of mission control, served by the HTTP server of the JDK: the page itself (mission-control.html),
// which asks every second for the state of the missions (/missions, as JSON) and draws them.
final class WebPage {
    private final HttpServer server;

    WebPage(int port, Supplier<Object> missions) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/missions", exchange -> send(exchange, "application/json",
                TelemetryJson.write(missions.get()).getBytes(StandardCharsets.UTF_8)));
        server.createContext("/", exchange -> {
            try (InputStream page = WebPage.class.getResourceAsStream("/mission-control.html")) {
                send(exchange, "text/html; charset=utf-8", page.readAllBytes());
            }
        });
    }

    void start() {
        server.start();
    }

    void stop() {
        server.stop(0);
    }

    private static void send(HttpExchange exchange, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
