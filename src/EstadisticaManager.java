import java.util.concurrent.ConcurrentLinkedQueue;

public class EstadisticaManager implements Runnable {
    private final ConcurrentLinkedQueue<Peticion> cola;
    private volatile boolean running = true;
    private int exitosas = 0, fallidas = 0, total = 0;

    public EstadisticaManager(ConcurrentLinkedQueue<Peticion> cola) {
        this.cola = cola;
    }

    public void run() {
        while (running || !cola.isEmpty()) {
            Peticion p = cola.poll();
            if (p != null) {
                total++;
                if (p.exito) exitosas++;
                else fallidas++;
            } else {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }
    }

    public void stop() { running = false; }
    public int getExitosas() { return exitosas; }
    public int getFallidas() { return fallidas; }
    public double getPorcentajeExito() { return total == 0 ? 0 : exitosas * 100.0 / total; }
    public double getPorcentajeFallo() { return total == 0 ? 0 : fallidas * 100.0 / total; }

    public static class Peticion {
        public final int id;
        public final boolean exito;
        public final String mensaje;
        public Peticion(int id, boolean exito, String mensaje) {
            this.id = id;
            this.exito = exito;
            this.mensaje = mensaje;
        }
    }
}
