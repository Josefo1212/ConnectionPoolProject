import java.sql.DriverManager;
import java.sql.Connection;
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

    void escucharFreno() {
        new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    int input = System.in.read();
                    if (input == 'q' || input == 'Q') {
                        IO.println("\nFreno de emergencia activado por el usuario. Deteniendo todas las peticiones...");
                        activarFreno();
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }, "FrenoThread").start();
    }

    void ejecutarSinPool() {
        freno.set(false);
        escucharFreno();
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
                        var rs = stmt.executeQuery("SELECT * FROM pg_catalog.pg_tables LIMIT 1");
                        while (rs.next() && !estaFrenado()) {
                            IO.println("[Sin pool] Petición " + idx + ": tabla = " + rs.getString("tablename"));
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
        var fin = System.currentTimeMillis();
        IO.println("Tiempo total sin pool: " + (fin - inicio) + " ms");
    }

    void ejecutarConPool() {
        freno.set(false);
        escucharFreno();
        var inicio = System.currentTimeMillis();
        var hilos = new Thread[numeroPeticiones];
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            hilos[i] = new Thread(() -> {
                if (estaFrenado()) return;
                try {
                    var poolManager = new PoolManager();
                    var connection = poolManager.getConnection();
                    if (connection != null && !estaFrenado()) {
                        try (var stmt = connection.createStatement()) {
                            var rs = stmt.executeQuery("SELECT * FROM pg_catalog.pg_tables LIMIT 1");
                            while (rs.next() && !estaFrenado()) {
                                IO.println("[Con pool] Petición " + idx + ": tabla = " + rs.getString("tablename"));
                            }
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } else {
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
        var fin = System.currentTimeMillis();
        IO.println("Tiempo total con pool: " + (fin - inicio) + " ms");
    }
}
