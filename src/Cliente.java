import java.sql.DriverManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);
    private TextArea outputArea;
    private final ConcurrentLinkedQueue<String> mensajes = new ConcurrentLinkedQueue<>();

    public Cliente(int numeroPeticiones) {
        this.numeroPeticiones = numeroPeticiones;
    }

    public void setOutput(TextArea area) {
        this.outputArea = area;
        iniciarActualizador();
    }

    private void print(String msg) {
        mensajes.offer(msg);
    }

    private void iniciarActualizador() {
        Platform.runLater(() -> {
            var hilo = new Thread(() -> {
                while (!estaFrenado()) {
                    if (!mensajes.isEmpty() && outputArea != null) {
                        Platform.runLater(() -> {
                            while (!mensajes.isEmpty()) {
                                outputArea.appendText(mensajes.poll() + "\n");
                            }
                        });
                    }
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
            });
            hilo.setDaemon(true);
            hilo.start();
        });
    }

    public static void activarFreno() {
        freno.set(true);
    }

    public static boolean estaFrenado() {
        return freno.get();
    }

    Thread escucharFreno() {
        var t = new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    if (System.in.available() > 0) {
                        int input = System.in.read();
                        if (input == '\n' || input == '\r') {
                            print("\nFreno de emergencia activado por el usuario. Deteniendo todas las peticiones...");
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

    public void ejecutarSinPool() {
        freno.set(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones];
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            hilos[i] = new Thread(() -> {
                if (estaFrenado()) return;
                try (var connection = DriverManager.getConnection(
                        "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                        Config.get("DB_USER"),
                        Config.get("DB_PASSWORD")
                )) {
                    if (estaFrenado()) return;
                    try (var stmt = connection.createStatement()) {
                        var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                        while (rs.next() && !estaFrenado()) {
                            print("[Sin pool] Petición " + idx + ": usuario = " + rs.getString(1));
                        }
                    }
                    Thread.sleep(new Random().nextInt(500));
                } catch (Exception e) {
                    print("Error en el cliente (sin pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total sin pool: " + (fin - inicio) + " ms");
    }

    public void ejecutarConPool() {
        freno.set(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones];
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            hilos[i] = new Thread(() -> {
                if (estaFrenado()) return;
                try {
                    var poolManager = new PoolManager();
                    if (estaFrenado()) return;
                    var connection = poolManager.getConnection();
                    if (estaFrenado()) {
                        if (connection != null) poolManager.releaseConnection(connection);
                        return;
                    }
                    if (connection != null) {
                        try (var stmt = connection.createStatement()) {
                            var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                            while (rs.next() && !estaFrenado()) {
                                print("[Con pool] Petición " + idx + ": usuario = " + rs.getString(1));
                            }
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } else if (!estaFrenado()) {
                        print("No se pudo obtener una conexión del pool en la petición " + idx);
                    }
                } catch (Exception e) {
                    print("Error en el cliente (con pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total con pool: " + (fin - inicio) + " ms");
    }

    public static class Estadisticas {
        public int exitosas = 0;
        public int fallidas = 0;
        public int total = 0;
        public double porcentajeExito() {
            return total == 0 ? 0 : (exitosas * 100.0 / total);
        }
        public double porcentajeFallo() {
            return total == 0 ? 0 : (fallidas * 100.0 / total);
        }
    }

    public Estadisticas ejecutarSinPoolConEstadisticas() {
        freno.set(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones];
        var stats = new Estadisticas();
        stats.total = numeroPeticiones;
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            hilos[i] = new Thread(() -> {
                if (estaFrenado()) return;
                try (var connection = DriverManager.getConnection(
                        "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                        Config.get("DB_USER"),
                        Config.get("DB_PASSWORD")
                )) {
                    if (estaFrenado()) return;
                    try (var stmt = connection.createStatement()) {
                        var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                        boolean exito = false;
                        while (rs.next() && !estaFrenado()) {
                            exito = true;
                            print("[Sin pool] Petición " + idx + ": usuario = " + rs.getString(1));
                        }
                        if (exito) stats.exitosas++;
                        else stats.fallidas++;
                    }
                    Thread.sleep(new Random().nextInt(500));
                } catch (Exception e) {
                    stats.fallidas++;
                    print("Error en el cliente (sin pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total sin pool: " + (fin - inicio) + " ms");
        return stats;
    }

    public Estadisticas ejecutarConPoolConEstadisticas() {
        freno.set(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones];
        var stats = new Estadisticas();
        stats.total = numeroPeticiones;
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            hilos[i] = new Thread(() -> {
                if (estaFrenado()) return;
                try {
                    var poolManager = new PoolManager();
                    if (estaFrenado()) return;
                    var connection = poolManager.getConnection();
                    if (estaFrenado()) {
                        if (connection != null) poolManager.releaseConnection(connection);
                        return;
                    }
                    if (connection != null) {
                        try (var stmt = connection.createStatement()) {
                            var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                            boolean exito = false;
                            while (rs.next() && !estaFrenado()) {
                                exito = true;
                                print("[Con pool] Petición " + idx + ": usuario = " + rs.getString(1));
                            }
                            if (exito) stats.exitosas++;
                            else stats.fallidas++;
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } else if (!estaFrenado()) {
                        stats.fallidas++;
                        print("No se pudo obtener una conexión del pool en la petición " + idx);
                    }
                } catch (Exception e) {
                    stats.fallidas++;
                    print("Error en el cliente (con pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total con pool: " + (fin - inicio) + " ms");
        return stats;
    }
}
