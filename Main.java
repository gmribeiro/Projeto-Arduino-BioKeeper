/**
 * =====================================================================
 *  BIO KEEPER — Bridge Serial → REST API
 *  FATEC Indaiatuba — ADS 2026
 * =====================================================================
 *
 *  Fluxo produção:  ESP32 → SerialReader → DataStore → HTTP → Dashboard
 *  Fluxo de teste:            Esp32Simulator → DataStore → HTTP → Dashboard
 *
 *  Dependência externa (único JAR necessário):
 *    jSerialComm-2.x.jar  →  https://fazecast.github.io/jSerialComm/
 *
 *  Como rodar no IntelliJ:
 *    Main class        : Main
 *    Program arguments : --sim              (teste sem ESP32)
 *                        COM3               (ESP32 real)
 *                        COM3 115200 8080   (ESP32 real, porta e baud explícitos)
 *                        --list             (lista portas COM disponíveis)
 * =====================================================================
 */
public class Main {

    private static final int DEFAULT_BAUD      = 115200;
    private static final int DEFAULT_HTTP_PORT = 8080;

    public static void main(String[] args) throws Exception {

        // ── Sem argumentos: mostra ajuda ─────────────────────────────
        if (args.length == 0) {
            printUsage();
            return;
        }

        String primeiro = args[0];

        // ── --sim: simulador interno, não usa porta serial ────────────
        if (primeiro.equals("--sim")) {
            int httpPort = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_HTTP_PORT;

            System.out.println("=== BIO KEEPER — Modo Simulado (sem ESP32) ===");
            System.out.printf("  API REST : http://localhost:%d/api/%n%n", httpPort);

            BioKeeperHttpServer httpServer = new BioKeeperHttpServer(httpPort);
            httpServer.start();

            Esp32Simulator sim = new Esp32Simulator();
            Thread simThread = new Thread(sim, "esp32-simulator");
            simThread.setDaemon(false);
            simThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Main] Encerrando simulador...");
                sim.stop();
                httpServer.stop();
            }));
            return;
        }

        // ── --list: lista portas COM e sai ────────────────────────────
        if (primeiro.equals("--list")) {
            SerialReader.listPorts();
            return;
        }

        // ── Modo real: lê do ESP32 via porta serial ───────────────────
        String portName = primeiro;
        int    baud     = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_BAUD;
        int    httpPort = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_HTTP_PORT;

        System.out.println("=== BIO KEEPER — Bridge Serial → REST API ===");
        System.out.printf("  Porta serial : %s @ %d baud%n", portName, baud);
        System.out.printf("  API REST     : http://localhost:%d/api/%n%n", httpPort);

        BioKeeperHttpServer httpServer = new BioKeeperHttpServer(httpPort);
        httpServer.start();

        SerialReader reader = new SerialReader(portName, baud, DataStore::ingest);
        Thread serialThread = new Thread(reader, "serial-reader");
        serialThread.setDaemon(false);
        serialThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] Encerrando...");
            reader.stop();
            httpServer.stop();
        }));
    }

    private static void printUsage() {
        System.out.println("=== BIO KEEPER — Uso ===");
        System.out.println("  --sim              Roda com ESP32 simulado (para testes)");
        System.out.println("  --sim 9090         Simulado na porta HTTP 9090");
        System.out.println("  --list             Lista portas COM disponíveis");
        System.out.println("  COM3               Lê do ESP32 na COM3");
        System.out.println("  COM3 115200 8080   COM3, baud e porta HTTP explícitos");
    }
}
