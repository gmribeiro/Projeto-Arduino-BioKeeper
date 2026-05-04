import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * SerialReader
 * ------------
 * Abre a porta COM do ESP32, lê linha a linha e repassa
 * cada JSON recebido para o DataStore via callback.
 *
 * Reconecta automaticamente se a porta cair ou o cabo for removido.
 */
public class SerialReader implements Runnable {

    private final String           portName;
    private final int              baudRate;
    private final Consumer<String> onLine;

    private volatile boolean running = true;

    public SerialReader(String portName, int baudRate, Consumer<String> onLine) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.onLine   = onLine;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        while (running) {
            SerialPort port = SerialPort.getCommPort(portName);
            port.setBaudRate(baudRate);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

            System.out.println("[Serial] Conectando em " + portName + " @ " + baudRate + " baud...");

            if (!port.openPort()) {
                System.err.println("[Serial] Falha ao abrir " + portName + ". Tentando em 3s...");
                sleep(3000);
                continue;
            }

            System.out.println("[Serial] Conectado.");
            DataStore.setSerialConnected(true);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(port.getInputStream()))) {

                String line;
                while (running && (line = reader.readLine()) != null) {
                    line = line.trim();
                    // Ignora linhas de debug do ESP32; processa só JSON
                    if (line.startsWith("{")) {
                        onLine.accept(line);
                    }
                }

            } catch (Exception e) {
                System.err.println("[Serial] Erro de leitura: " + e.getMessage());
            } finally {
                port.closePort();
                DataStore.setSerialConnected(false);
                System.err.println("[Serial] Porta fechada. Reconectando em 3s...");
                sleep(3000);
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    // Lista as portas COM disponíveis no sistema (usado com --list)
    public static void listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("Nenhuma porta serial encontrada.");
            return;
        }
        System.out.println("Portas disponíveis:");
        for (SerialPort p : ports) {
            System.out.println("  " + p.getSystemPortName()
                    + " — " + p.getDescriptivePortName());
        }
    }
}
