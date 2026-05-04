import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * BioKeeperHttpServer
 * -------------------
 * Servidor HTTP leve usando a API interna do JDK (sem dependência externa).
 * Expõe os endpoints REST que o dashboard consome via polling.
 *
 * Endpoints:
 *   GET /api/latest          → última leitura em JSON
 *   GET /api/history?limit=N → últimas N leituras em JSON (padrão: 100)
 *   GET /api/status          → status da serial + contadores
 *   GET /api/export/csv      → histórico completo para download
 */
public class BioKeeperHttpServer {

    private final int        port;
    private       HttpServer server;

    public BioKeeperHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/latest",     this::handleLatest);
        server.createContext("/api/history",    this::handleHistory);
        server.createContext("/api/status",     this::handleStatus);
        server.createContext("/api/export/csv", this::handleCsv);

        // Thread pool para suportar múltiplos clientes simultâneos
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("[HTTP] API REST disponível em http://localhost:" + port + "/api/");
        System.out.println("[HTTP] Endpoints disponíveis:");
        System.out.println("         GET /api/latest");
        System.out.println("         GET /api/history?limit=100");
        System.out.println("         GET /api/status");
        System.out.println("         GET /api/export/csv");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ── Handlers ─────────────────────────────────────────────────────

    private void handleLatest(HttpExchange ex) throws IOException {
        if (!isGet(ex)) { send405(ex); return; }
        sendJson(ex, DataStore.getLatestJson());
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        if (!isGet(ex)) { send405(ex); return; }
        int limit = parseQueryParam(ex.getRequestURI(), "limit", 100);
        sendJson(ex, DataStore.getHistoryJson(limit));
    }

    private void handleStatus(HttpExchange ex) throws IOException {
        if (!isGet(ex)) { send405(ex); return; }
        sendJson(ex, DataStore.getStatusJson());
    }

    private void handleCsv(HttpExchange ex) throws IOException {
        if (!isGet(ex)) { send405(ex); return; }
        byte[] body = DataStore.getHistoryCsv().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",        "text/csv; charset=utf-8");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"biokeeper_log.csv\"");
        addCors(ex);
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        addCors(ex);
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private void send405(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(405, -1);
        ex.getResponseBody().close();
    }

    // Habilita o dashboard (rodando em outra origem) consumir a API
    private void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private boolean isGet(HttpExchange ex) {
        return "GET".equalsIgnoreCase(ex.getRequestMethod());
    }

    private int parseQueryParam(URI uri, String key, int defaultValue) {
        String query = uri.getQuery();
        if (query == null) return defaultValue;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try { return Integer.parseInt(kv[1]); }
                catch (NumberFormatException ignored) { }
            }
        }
        return defaultValue;
    }
}
