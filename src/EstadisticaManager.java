import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EstadisticaManager implements Runnable {
    public static class Peticion {
        public final int id;
        public final boolean exito;
        public final String motivo;
        public Peticion(int id, boolean exito, String motivo) {
            this.id = id;
            this.exito = exito;
            this.motivo = motivo;
        }
    }

    private final ConcurrentLinkedQueue<Peticion> cola;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile int exitosas = 0;
    private volatile int fallidas = 0;
    private volatile int total = 0;

    public EstadisticaManager(ConcurrentLinkedQueue<Peticion> cola) {
        this.cola = cola;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        int intentos = 0;
        while (running.get() || !cola.isEmpty()) {
            var peticion = cola.poll();
            if (peticion != null) {
                synchronized (this) {
                    total++;
                    if (peticion.exito) exitosas++;
                    else fallidas++;
                }
                intentos = 0;
            } else {
                // Si la cola está vacía y el freno está activado, termina después de varios intentos
                if (!running.get() && cola.isEmpty()) {
                    intentos++;
                    if (intentos > 20) break;
                }
            }
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
    }

    public synchronized int getExitosas() { return exitosas; }
    public synchronized int getFallidas() { return fallidas; }
    public synchronized int getTotal() { return total; }
    public synchronized double getPorcentajeExito() { return total == 0 ? 0 : exitosas * 100.0 / total; }
    public synchronized double getPorcentajeFallo() { return total == 0 ? 0 : fallidas * 100.0 / total; }
}
