import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DataStore
 * ---------
 * Recebe linhas JSON do SerialReader ou Esp32Simulator,
 * faz parse e armazena thread-safe.
 *
 * Endpoints servidos:
 *   GET /api/latest          → getLatestJson()
 *   GET /api/history?limit=N → getHistoryJson(n)
 *   GET /api/status          → getStatusJson()
 *   GET /api/export/csv      → getHistoryCsv()
 */
public class DataStore {

    private static final int MAX_HISTORY = 500;

    private static final ReadWriteLock lock    = new ReentrantReadWriteLock();
    private static       String        latest  = "{}";
    private static final Deque<String> history = new ArrayDeque<>();

    private static final AtomicBoolean serialConnected = new AtomicBoolean(false);
    private static final AtomicLong    totalReadings   = new AtomicLong(0);
    private static final AtomicLong    downtimeCount   = new AtomicLong(0);

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // ── Ingestão ──────────────────────────────────────────────────────
    public static void ingest(String rawLine) {
        try {
            float   temp = getFloat(rawLine, "temp");
            float   umid = getFloat(rawLine, "umid");
            int     luzR = (int) getLong(rawLine, "luz_r");
            int     luzG = (int) getLong(rawLine, "luz_g");
            int     luzB = (int) getLong(rawLine, "luz_b");
            long    ts   = getLong(rawLine, "ts");
            boolean down = getBool(rawLine, "down");

            String iso = (ts > 0)
                    ? FMT.format(Instant.ofEpochSecond(ts))
                    : FMT.format(Instant.now());

            String enriched = String.format(
                    "{\"temp\":%.2f,\"umid\":%.2f," +
                    "\"luz_r\":%d,\"luz_g\":%d,\"luz_b\":%d," +
                    "\"ts\":%d,\"iso\":\"%s\",\"down\":%s}",
                    temp, umid, luzR, luzG, luzB, ts, iso,
                    down ? "true" : "false"
            );

            lock.writeLock().lock();
            try {
                latest = enriched;
                history.addFirst(enriched);
                if (history.size() > MAX_HISTORY) history.removeLast();
            } finally {
                lock.writeLock().unlock();
            }

            totalReadings.incrementAndGet();
            if (down) downtimeCount.incrementAndGet();

            System.out.printf("[Leitura #%d] T=%.1f°C  U=%.1f%%  RGB=(%d,%d,%d)%s%n",
                    totalReadings.get(), temp, umid, luzR, luzG, luzB,
                    down ? "  ⚡DOWNTIME" : "");

        } catch (Exception e) {
            System.err.println("[DataStore] Erro ao parsear: " + rawLine + " — " + e.getMessage());
        }
    }

    // ── Leituras ──────────────────────────────────────────────────────
    public static String getLatestJson() {
        lock.readLock().lock();
        try { return latest; }
        finally { lock.readLock().unlock(); }
    }

    public static String getHistoryJson(int limit) {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (String entry : history) {
                if (count >= limit) break;
                if (count > 0) sb.append(",");
                sb.append(entry);
                count++;
            }
            return sb.append("]").toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static String getHistoryCsv() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder(
                    "iso_time,temp,umid,luz_r,luz_g,luz_b,ts,downtime\n");
            String[] arr = history.toArray(new String[0]);
            // inverte para ordem cronológica
            for (int i = arr.length - 1; i >= 0; i--) {
                String e = arr[i];
                sb.append(getStr(e, "iso")).append(",")
                  .append(getFloat(e, "temp")).append(",")
                  .append(getFloat(e, "umid")).append(",")
                  .append(getLong(e, "luz_r")).append(",")
                  .append(getLong(e, "luz_g")).append(",")
                  .append(getLong(e, "luz_b")).append(",")
                  .append(getLong(e, "ts")).append(",")
                  .append(getBool(e, "down") ? "1" : "0").append("\n");
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static String getStatusJson() {
        return String.format(
                "{\"serial_ok\":%s,\"total\":%d,\"downtimes\":%d}",
                serialConnected.get(), totalReadings.get(), downtimeCount.get()
        );
    }

    public static void setSerialConnected(boolean v) { serialConnected.set(v); }

    // ── Parser JSON manual (sem dependência externa) ──────────────────
    private static float getFloat(String json, String key) {
        int pos = json.indexOf("\"" + key + "\"");
        if (pos < 0) return 0f;
        pos = json.indexOf(':', pos) + 1;
        return Float.parseFloat(json.substring(pos, nextDelimiter(json, pos)).trim());
    }

    private static long getLong(String json, String key) {
        int pos = json.indexOf("\"" + key + "\"");
        if (pos < 0) return 0L;
        pos = json.indexOf(':', pos) + 1;
        return Long.parseLong(json.substring(pos, nextDelimiter(json, pos)).trim());
    }

    private static boolean getBool(String json, String key) {
        int pos = json.indexOf("\"" + key + "\"");
        if (pos < 0) return false;
        pos = json.indexOf(':', pos) + 1;
        String val = json.substring(pos, nextDelimiter(json, pos)).trim();
        return val.equals("1") || val.equals("true");
    }

    private static String getStr(String json, String key) {
        int pos = json.indexOf("\"" + key + "\":\"");
        if (pos < 0) return "";
        pos += key.length() + 4;
        int end = json.indexOf('"', pos);
        return end < 0 ? "" : json.substring(pos, end);
    }

    private static int nextDelimiter(String json, int from) {
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') return i;
        }
        return json.length();
    }
}
