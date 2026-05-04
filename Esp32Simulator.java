import java.time.Instant;
import java.time.LocalTime;
import java.util.Random;

/**
 * Esp32Simulator
 * --------------
 * Simula o ESP32 sem porta serial nem driver externo.
 * Gera leituras realistas e injeta direto no DataStore,
 * exatamente como o SerialReader faria em produção.
 *
 * Comportamento:
 *   - Temperatura oscila em torno de 8°C (típico de refrigeração)
 *   - Umidade oscila em torno de 70%
 *   - LEDs azul+verde ativos entre 06h–20h (hora local da máquina)
 *   - Downtime aleatório ~5% das leituras
 *   - Intervalo: 2 segundos (igual ao ESP32 real)
 */
public class Esp32Simulator implements Runnable {

    private static final int INTERVAL_MS = 2000;

    private final Random         rng     = new Random();
    private volatile boolean     running = true;

    private float temp = 8.0f;
    private float umid = 70.0f;

    public void stop() { running = false; }

    @Override
    public void run() {
        System.out.println("[Simulador] ESP32 simulado iniciado (Ctrl+C para parar).");
        System.out.println("[Simulador] Enviando leituras a cada " + INTERVAL_MS + "ms...\n");

        while (running) {
            String json = buildJson();
            System.out.println("[Simulador] → " + json);
            DataStore.ingest(json);

            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private String buildJson() {
        // Random walk suave
        temp = clamp(temp + (rng.nextFloat() - 0.5f) * 0.4f, 4f, 14f);
        umid = clamp(umid + (rng.nextFloat() - 0.5f) * 1.0f, 55f, 90f);

        // Ciclo de luz baseado na hora local
        int hour = LocalTime.now().getHour();
        int luzR = 0;
        int luzG = (hour >= 6 && hour < 20) ? 120 : 0;
        int luzB = (hour >= 6 && hour < 20) ? 150 : 0;

        boolean down = rng.nextFloat() < 0.05f;
        long    ts   = Instant.now().getEpochSecond();

        return String.format(
                "{\"temp\":%.2f,\"umid\":%.2f," +
                "\"luz_r\":%d,\"luz_g\":%d,\"luz_b\":%d," +
                "\"ts\":%d,\"down\":%d}",
                temp, umid, luzR, luzG, luzB, ts, down ? 1 : 0
        );
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
