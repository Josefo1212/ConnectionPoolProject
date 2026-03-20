import adapters.DatabaseType;
import dbcomponent.DBComponentRegistry;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Orquesta la simulacion fuera de la UI.
 */
public final class SimulacionService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void activarFreno(boolean estado) {
        Cliente.activarFreno(estado);
    }

    public void ejecutarConPool(DatabaseType type,
                                int totalPeticiones,
                                Consumer<ProgressView> onProgress,
                                Consumer<ResultadoView> onResultado,
                                Consumer<String> onInfo) {
        if (type == null) {
            throw new IllegalArgumentException("Selecciona PostgreSQL o MySQL antes de simular");
        }
        if (totalPeticiones <= 0) {
            throw new IllegalArgumentException("Ingresa un numero mayor a 0");
        }
        if (!DBComponentRegistry.isConnected(type)) {
            throw new IllegalStateException("Primero conecta a " + type + " (ingresa datos y presiona Conectar)");
        }

        Thread worker = new Thread(() -> runSimulation(type, totalPeticiones, onProgress, onResultado, onInfo), "SimulacionPoolThread");
        worker.start();
    }

    private void runSimulation(DatabaseType type,
                               int totalPeticiones,
                               Consumer<ProgressView> onProgress,
                               Consumer<ResultadoView> onResultado,
                               Consumer<String> onInfo) {
        try {
            Cliente.setDatabaseType(type);
            Cliente.activarFreno(false);

            ConcurrentLinkedQueue<EstadisticaManager.Peticion> cola = new ConcurrentLinkedQueue<>();
            EstadisticaManager manager = new EstadisticaManager(cola);
            Thread hiloManager = new Thread(manager, "EstadisticaManagerThread");
            Cliente clientePool = new Cliente(totalPeticiones);
            clientePool.setEstadisticaQueue(cola);

            CountDownLatch terminado = new CountDownLatch(1);
            final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];
            future[0] = scheduler.scheduleAtFixedRate(() -> {
                int completadas = clientePool.getCompletadas();
                double progreso = Math.min(completadas / (double) totalPeticiones, 1.0);
                onProgress.accept(new ProgressView(completadas, totalPeticiones, progreso));

                if (completadas >= totalPeticiones || Cliente.estaFrenado()) {
                    if (future[0] != null) future[0].cancel(false);
                    if (terminado.getCount() > 0) terminado.countDown();
                }
            }, 0, 20, TimeUnit.MILLISECONDS);

            hiloManager.start();
            clientePool.ejecutarSimulacionConEstadisticas();
            manager.stop();
            try {
                hiloManager.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try {
                boolean done = terminado.await(2, TimeUnit.SECONDS);
                if (!done) {
                    onInfo.accept("La simulacion tardo mas de lo esperado en finalizar.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int ex = manager.getExitosas();
            int fa = manager.getFallidas();
            double exitoPct = manager.getPorcentajeExito();
            onResultado.accept(new ResultadoView(ex, fa, exitoPct));
        } catch (Exception e) {
            onInfo.accept("Error en simulacion: " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public record ProgressView(int completadas, int total, double progreso) {
    }

    public record ResultadoView(int exitosas, int fallidas, double porcentajeExito) {
    }
}

