import java.sql.DriverManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);

    Cliente(int numeroPeticiones) {
        this.numeroPeticiones = numeroPeticiones;
    }

    static void activarFreno() {
        freno.set(true);
    }

    static boolean estaFrenado() {
        return freno.get();
    }

    Thread escucharFreno() {
        var t = new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    if (System.in.available() > 0) {
                        int input = System.in.read();
                        if (input == '\n' || input == '\r') { // Enter en Unix y Windows
                            IO.println("\nFreno de emergencia activado por el usuario. Deteniendo todas las peticiones...");
                            activarFreno();
                            break;
                        }
                    }
                    Thread.sleep(50); // Pequeña pausa para evitar alto uso de CPU
                }
            } catch (Exception ignored) {}
        }, "FrenoThread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    void ejecutarSinPool() {
        freno.set(false); // Reinicia el freno antes de la simulación
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
                            IO.println("[Sin pool] Petición " + idx + ": usuario = " + rs.getString(1));
                        }
                    }
                    Thread.sleep(new Random().nextInt(500));
                } catch (Exception e) {
                    IO.println("Error en el cliente (sin pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true); // Detener el hilo de escucha
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        IO.println("Tiempo total sin pool: " + (fin - inicio) + " ms");
    }

    void ejecutarConPool() {
        freno.set(false); // Reinicia el freno antes de la simulación
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones]; // Corrige la declaración del array
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
                                IO.println("[Con pool] Petición " + idx + ": usuario = " + rs.getString(1));
                            }
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } // No else: si no hay conexión y el freno está activo, no mostrar mensaje
                    else if (!estaFrenado()) {
                        IO.println("No se pudo obtener una conexión del pool en la petición " + idx);
                    }
                } catch (Exception e) {
                    IO.println("Error en el cliente (con pool), petición " + idx + ": " + e.getMessage());
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            try { hilo.join(); } catch (InterruptedException ignored) {}
        }
        freno.set(true); // Detener el hilo de escucha
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        IO.println("Tiempo total con pool: " + (fin - inicio) + " ms");
    }
}
