import java.sql.DriverManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import pool.PoolManager;

import adapters.DatabaseType;
import adapters.mysql.MySQLConfig;
import adapters.postgres.PostgreSQLConfig;

public class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);

    private static volatile DatabaseType databaseType; // se setea desde la UI antes de simular

    private TextArea outputArea;
    private ConcurrentLinkedQueue<EstadisticaManager.Peticion> colaEstadisticas;
    private final AtomicInteger completadas = new AtomicInteger(0);
    private final AtomicInteger exitosas = new AtomicInteger(0);
    private final AtomicInteger fallidas = new AtomicInteger(0);
    private PoolManager poolManager;

    public static void setDatabaseType(DatabaseType type) {
        databaseType = type;
    }

    private static DatabaseType requireDatabaseType() {
        if (databaseType == null) {
            throw new IllegalStateException("No se seleccionó base de datos. Elige PostgreSQL o MySQL antes de simular.");
        }
        return databaseType;
    }

    private static String resolveJdbcUrl() {
        return switch (requireDatabaseType()) {
            case POSTGRES -> PostgreSQLConfig.URL;
            case MYSQL -> MySQLConfig.URL;
        };
    }

    private static String resolveUser() {
        return switch (requireDatabaseType()) {
            case POSTGRES -> PostgreSQLConfig.USER;
            case MYSQL -> MySQLConfig.USER;
        };
    }

    private static String resolvePassword() {
        return switch (requireDatabaseType()) {
            case POSTGRES -> PostgreSQLConfig.PASSWORD;
            case MYSQL -> MySQLConfig.PASSWORD;
        };
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
                    Thread.sleep(50);
                }
            } catch (Exception ignored) {}
        }, "FrenoThread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void ejecutarSinPoolConEstadisticas() {
        activarFreno(false);
        var frenoThread = escucharFreno();
        completadas.set(0);
        exitosas.set(0);
        fallidas.set(0);
        var inicio = System.currentTimeMillis();
        var tareas = new java.util.ArrayList<Thread>();

        // Resolver credenciales según selección (NO pool)
        String url = resolveJdbcUrl();
        String user = resolveUser();
        String pass = resolvePassword();

        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            Thread t = new Thread(() -> {
                boolean exito = false;
                try (var connection = DriverManager.getConnection(url, user, pass)) {
                    if (estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        fallidas.incrementAndGet();
                        return;
                    }
                    try (var stmt = connection.createStatement()) {
                        var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                        while (rs.next() && !estaFrenado()) {
                            exito = true;
                        }
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados"));
                        if (exito) exitosas.incrementAndGet();
                        else fallidas.incrementAndGet();
                    }
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
        if (outputArea != null) Platform.runLater(() -> outputArea.appendText("Tiempo total sin pool: " + (fin - inicio) + " ms\n"));
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
                boolean exito = false;
                try {
                    if (poolManager == null) poolManager = PoolManager.getInstance();
                    if (estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        fallidas.incrementAndGet();
                        return;
                    }
                    var connection = poolManager.getConnection();
                    if (estaFrenado()) {
                        if (connection != null) poolManager.releaseConnection(connection);
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        fallidas.incrementAndGet();
                        return;
                    }
                    if (connection != null) {
                        try (var stmt = connection.createStatement()) {
                            var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                            while (rs.next() && !estaFrenado()) {
                                exito = true;
                            }
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados"));
                            if (exito) exitosas.incrementAndGet();
                            else fallidas.incrementAndGet();
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } else if (!estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "No se pudo obtener conexión"));
                        fallidas.incrementAndGet();
                    }
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
        if (outputArea != null) Platform.runLater(() -> outputArea.appendText("Tiempo total con pool: " + (fin - inicio) + " ms\n"));
    }
}
