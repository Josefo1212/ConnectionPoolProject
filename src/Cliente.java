import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import adapters.DatabaseType;
import dbcomponent.DBComponent;
import dbcomponent.DBComponentRegistry;
import dbcomponent.DBQueryId;

public class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);

    private static volatile DatabaseType databaseType; // se setea desde la UI antes de simular

    private static final DBQueryId Q_SELECT_ONE = new DBQueryId("usuario.selectOne");

    private TextArea outputArea;
    private ConcurrentLinkedQueue<EstadisticaManager.Peticion> colaEstadisticas;
    private final AtomicInteger completadas = new AtomicInteger(0);
    private final AtomicInteger exitosas = new AtomicInteger(0);
    private final AtomicInteger fallidas = new AtomicInteger(0);

    public static void setDatabaseType(DatabaseType type) {
        databaseType = type;
    }

    private static DatabaseType requireDatabaseType() {
        if (databaseType == null) {
            throw new IllegalStateException("No se seleccionó base de datos. Elige PostgreSQL o MySQL antes de simular.");
        }
        return databaseType;
    }

    private static DBComponent requireComponent() {
        DatabaseType t = requireDatabaseType();
        DBComponent c = DBComponentRegistry.get(t);
        if (c == null) {
            throw new IllegalStateException("No hay conexión activa. Presiona Conectar antes de simular.");
        }
        return c;
    }


    public Cliente(int numeroPeticiones) {
        this.numeroPeticiones = numeroPeticiones;
    }

    public void setOutput(TextArea area) { this.outputArea = area; }
    public void setEstadisticaQueue(ConcurrentLinkedQueue<EstadisticaManager.Peticion> queue) { this.colaEstadisticas = queue; }
    public int getCompletadas() { return completadas.get(); }

    public static void activarFreno(boolean estado) { freno.set(estado); }
    public static void activarFreno() { activarFreno(true); }
    public static boolean estaFrenado() { return freno.get(); }

    private Thread escucharFreno() {
        var t = new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    if (System.in.available() > 0) {
                        int input = System.in.read();
                        if (input == '\n' || input == '\r') {
                            activarFreno();
                            break;
                        }
                    }
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                }
            } catch (Exception ignored) {}
        }, "FrenoThread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void ejecutarConPoolConEstadisticas() {
        // Asegura que se seleccionó DB antes de usar el pool
        requireDatabaseType();

        activarFreno(false);
        var frenoThread = escucharFreno();
        completadas.set(0);
        exitosas.set(0);
        fallidas.set(0);
        var inicio = System.currentTimeMillis();
        var tareas = new java.util.ArrayList<Thread>();
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            Thread t = new Thread(() -> {
                boolean exito;
                try {
                    // Con pool: obligatoriamente usar el DBComponent conectado para esta BD.
                    DBComponent comp = requireComponent();
                    if (estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        fallidas.incrementAndGet();
                        return;
                    }

                    // Ejecutar query predefinida por ID.
                    comp.query(Q_SELECT_ONE);
                    exito = true; // si no lanzó excepción, lo consideramos éxito
                    if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados"));
                    if (exito) exitosas.incrementAndGet();
                    else fallidas.incrementAndGet();

                    Thread.sleep(new Random().nextInt(500));
                } catch (Exception e) {
                    if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, e.getMessage()));
                    fallidas.incrementAndGet();
                } finally {
                    completadas.incrementAndGet();
                }
            });
            tareas.add(t);
            t.start();
        }
        for (var t : tareas) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        activarFreno(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        if (outputArea != null) Platform.runLater(() -> outputArea.appendText("Tiempo total simulación (pool): " + (fin - inicio) + " ms\n"));
    }

    // Alias semántico para reflejar que ahora solo existe ejecución con pool.
    public void ejecutarSimulacionConEstadisticas() {
        ejecutarConPoolConEstadisticas();
    }
}
