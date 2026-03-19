import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import adapters.DatabaseType;
import adapters.DBAdapterFactory;
import adapters.IDBAdapter;
import dbcomponent.ConnectionConfig;
import dbcomponent.DBComponent;
import dbcomponent.DBComponentRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Interfaz extends Application {
    private final VBox panelGrafica = new VBox();

    private final Label statsSinPool = new Label("0% Éxito");
    private final Label statsConPool = new Label("0% Éxito");

    private final ProgressBar progressSin = new ProgressBar(0);
    private final ProgressBar progressCon = new ProgressBar(0);

    private TextField txtPeticiones;
    private RadioButton rbPostgres, rbMySql;
    private Label estadoPostgres, estadoMySql;
    private Button btnSimular;

    // Campos conexión
    private TextField txtHost, txtPort, txtDb, txtUser;
    private PasswordField txtPass;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Progreso objetivo (lo que reporta la simulación)
    private final DoubleProperty targetProgresoSin = new SimpleDoubleProperty(0);
    private final DoubleProperty targetProgresoCon = new SimpleDoubleProperty(0);

    // Progreso mostrado (se interpola para verse fluido)
    private final DoubleProperty shownProgresoSin = new SimpleDoubleProperty(0);
    private final DoubleProperty shownProgresoCon = new SimpleDoubleProperty(0);

    private AnimationTimer smoothTimer;

    private final Label errorMsg = new Label("");

    @Override
    public void start(Stage stage) {
        // --- CONTENEDOR PRINCIPAL (HBox para dividir en 2 columnas) ---
        HBox root = new HBox(25);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-root");

        // ================= COLUMNA IZQUIERDA (Configuración) =================
        VBox leftCol = new VBox(12);
        // Evitar que se encoja cuando aparece la gráfica
        leftCol.setMinWidth(320);
        leftCol.setPrefWidth(320);
        leftCol.setMaxWidth(320);
        HBox.setHgrow(leftCol, Priority.NEVER);

        leftCol.getStyleClass().add("panel-oscuro");
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setFillWidth(true);

        Label titleLeft = new Label("Configuración de\nParámetros");
        titleLeft.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLeft.setTextFill(Color.WHITE);
        titleLeft.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Selector DB
        ToggleGroup tgDb = new ToggleGroup();
        rbPostgres = new RadioButton("PostgreSQL");
        rbMySql = new RadioButton("MySQL");
        rbPostgres.setToggleGroup(tgDb);
        rbMySql.setToggleGroup(tgDb);
        rbPostgres.getStyleClass().add("db-selector");
        rbMySql.getStyleClass().add("db-selector");
        HBox dbContainer = new HBox(12, rbPostgres, rbMySql);
        dbContainer.setAlignment(Pos.CENTER);

        rbPostgres.setOnAction(_ -> {
            rellenarDefaultsConexion(DatabaseType.POSTGRES);
            actualizarEstadoSimular();
        });
        rbMySql.setOnAction(_ -> {
            rellenarDefaultsConexion(DatabaseType.MYSQL);
            actualizarEstadoSimular();
        });

        Label lblDb = new Label("Base de Datos");
        lblDb.setTextFill(Color.LIGHTGRAY);

        Label lblEstado = new Label("Estado de conexión");
        lblEstado.setTextFill(Color.LIGHTGRAY);

        estadoPostgres = new Label();
        estadoPostgres.setWrapText(true);
        estadoPostgres.setMaxWidth(Double.MAX_VALUE);

        estadoMySql = new Label();
        estadoMySql.setWrapText(true);
        estadoMySql.setMaxWidth(Double.MAX_VALUE);

        // ================= FORMULARIO CONEXIÓN =================
        Label lblConn = new Label("Datos de conexión");
        lblConn.setTextFill(Color.LIGHTGRAY);

        txtHost = new TextField("");
        txtHost.setPromptText("host (ej. localhost)");
        txtHost.getStyleClass().add("custom-field");

        txtPort = new TextField("");
        txtPort.setPromptText("puerto (ej. 5432/3306)");
        txtPort.getStyleClass().add("custom-field");

        txtDb = new TextField("");
        txtDb.setPromptText("base de datos");
        txtDb.getStyleClass().add("custom-field");

        txtUser = new TextField("");
        txtUser.setPromptText("usuario");
        txtUser.getStyleClass().add("custom-field");

        txtPass = new PasswordField();
        txtPass.setPromptText("contraseña");
        txtPass.getStyleClass().add("custom-field");

        Button btnConectar = new Button("⛓ Conectar");
        btnConectar.getStyleClass().add("btn-iniciar");
        btnConectar.setMaxWidth(Double.MAX_VALUE);

        Button btnLimpiarConexion = new Button("🧹 Limpiar conexión actual");
        btnLimpiarConexion.getStyleClass().add("btn-freno");
        btnLimpiarConexion.setMaxWidth(Double.MAX_VALUE);

        // Control Peticiones
        Label lblPet = new Label("Número de Peticiones");
        lblPet.setTextFill(Color.LIGHTGRAY);
        txtPeticiones = new TextField("0");
        txtPeticiones.getStyleClass().add("custom-field");

        // Botones Acción
        btnSimular = new Button("▶ Iniciar");
        btnSimular.getStyleClass().add("btn-iniciar");
        btnSimular.setMaxWidth(Double.MAX_VALUE);

        Button btnFreno = new Button("■ Alto de emergencia");
        btnFreno.getStyleClass().add("btn-freno");
        btnFreno.setMaxWidth(Double.MAX_VALUE);

        errorMsg.setTextFill(Color.web("#ff4e8e"));
        errorMsg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        errorMsg.setWrapText(true);
        errorMsg.setMaxWidth(Double.MAX_VALUE);
        errorMsg.setAlignment(Pos.CENTER);
        errorMsg.setPadding(new Insets(8, 0, 0, 0));
        leftCol.getChildren().addAll(
                titleLeft,
                dbContainer,
                lblDb,
                lblEstado,
                estadoPostgres,
                estadoMySql,
                lblConn,
                txtHost,
                txtPort,
                txtDb,
                txtUser,
                txtPass,
                btnConectar,
                btnLimpiarConexion,
                lblPet,
                txtPeticiones,
                btnSimular,
                btnFreno,
                errorMsg
        );

        ScrollPane leftScroll = new ScrollPane(leftCol);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setPrefViewportWidth(330);
        leftScroll.setMinWidth(330);
        leftScroll.setMaxWidth(330);
        leftScroll.getStyleClass().add("left-scroll");

        // ================= COLUMNA DERECHA (Métricas) =================
        VBox rightCol = new VBox(20);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getStyleClass().add("panel-metriz");
        rightCol.setPadding(new Insets(20));

        Label titleRight = new Label("Métricas de Rendimiento en Tiempo Real");
        titleRight.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleRight.setTextFill(Color.web("#00ffff"));

        // Bindings de progreso (ahora se bindea al progreso 'shown' para suavizado)
        progressSin.progressProperty().bind(shownProgresoSin);
        progressCon.progressProperty().bind(shownProgresoCon);

        // Tarjetas KPI
        HBox kpiBox = new HBox(15);
        VBox cardSin = crearTarjetaKPI("Sin Pool", statsSinPool, progressSin, "#ff9966");
        VBox cardCon = crearTarjetaKPI("Con Pool", statsConPool, progressCon, "#00ffff");
        HBox.setHgrow(cardSin, Priority.ALWAYS);
        HBox.setHgrow(cardCon, Priority.ALWAYS);
        kpiBox.getChildren().addAll(cardSin, cardCon);

        // Área de Gráficas
        panelGrafica.setAlignment(Pos.CENTER);
        VBox.setVgrow(panelGrafica, Priority.ALWAYS);

        rightCol.getChildren().addAll(titleRight, kpiBox, panelGrafica);
        root.getChildren().addAll(leftScroll, rightCol);

        Scene scene = new Scene(root, 1120, 760);
        aplicarCSS(scene);

        refrescarIndicadoresConexion();
        actualizarEstadoSimular();

        stage.setTitle("Simulación Pool de Conexiones Pro");
        stage.setScene(scene);
        stage.show();

        startSmoothProgressAnimation();

        btnSimular.setOnAction(_ -> ejecutarSimulacion());
        btnConectar.setOnAction(_ -> conectarDB());
        btnLimpiarConexion.setOnAction(_ -> limpiarConexionActual());
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            errorMsg.setText("");
            Platform.runLater(() -> statsSinPool.setText("Freno de emergencia activado"));
        });

        stage.setOnCloseRequest(_ -> {
            try {
                scheduler.shutdownNow();
            } catch (Exception ignored) {
            }
            if (smoothTimer != null) smoothTimer.stop();
        });
    }

    private void startSmoothProgressAnimation() {
        if (smoothTimer != null) smoothTimer.stop();

        smoothTimer = new AnimationTimer() {
            // factor de suavizado (más alto = más rápido hacia el target)
            private static final double ALPHA = 0.18;

            @Override
            public void handle(long now) {
                smoothStep(shownProgresoSin, targetProgresoSin, ALPHA);
                smoothStep(shownProgresoCon, targetProgresoCon, ALPHA);
            }

            private void smoothStep(DoubleProperty shown, DoubleProperty target, double alpha) {
                double s = shown.get();
                double t = clamp01(target.get());
                double next = s + (t - s) * alpha;

                // snap al final para evitar quedarse "temblando" cerca de 1
                if (Math.abs(t - next) < 0.002) next = t;
                shown.set(clamp01(next));
            }

            private double clamp01(double v) {
                if (v < 0) return 0;
                if (v > 1) return 1;
                return v;
            }
        };

        smoothTimer.start();
    }

    private VBox crearTarjetaKPI(String titulo, Label val, ProgressBar pb, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15; -fx-border-color: " + color + "; -fx-border-radius: 15;");
        card.setMinWidth(220);
        card.setPrefWidth(260);
        card.setMaxWidth(420); // más ancho para texto largo

        Label t = new Label(titulo);
        t.setTextFill(Color.LIGHTGRAY);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        t.setWrapText(true);
        t.setMaxWidth(Double.MAX_VALUE);
        t.setAlignment(Pos.CENTER);

        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        val.setTextFill(Color.web(color));
        val.setWrapText(true);
        val.setMaxWidth(400); // más ancho para texto largo
        val.setMinWidth(220);
        val.setAlignment(Pos.CENTER);

        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: " + color + ";");

        card.getChildren().addAll(t, val, pb);
        return card;
    }

    private void aplicarCSS(Scene scene) {
        String style = """
            .main-root { -fx-background-color: #0d1117; }
            .panel-oscuro { -fx-background-color: #161b22; -fx-background-radius: 20; -fx-padding: 16; }
            .panel-metriz { -fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-radius: 20; -fx-border-width: 2; }
            .custom-field { -fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d; -fx-border-radius: 5; -fx-alignment: center; -fx-font-size: 14; -fx-padding: 6 8 6 8; }
            .btn-iniciar { -fx-background-color: linear-gradient(to bottom, #1f6feb, #0969da); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9; }
            .btn-freno { -fx-background-color: #8b0000; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 9; }
            .db-selector { -fx-text-fill: white; }
            .left-scroll { -fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0; }
            .left-scroll > .viewport { -fx-background-color: transparent; }
        """;

        scene.getStylesheets().add("data:text/css," + style.replace("\n", ""));
    }

    private void conectarDB() {
        Platform.runLater(() -> {
            errorMsg.setText("");
            errorMsg.setTextFill(Color.web("#ff4e8e"));
        });
        DatabaseType selected;
        if (rbPostgres.isSelected()) selected = DatabaseType.POSTGRES;
        else if (rbMySql.isSelected()) selected = DatabaseType.MYSQL;
        else {
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#ff4e8e"));
                errorMsg.setText("Selecciona PostgreSQL o MySQL antes de conectar");
            });
            return;
        }

        String host = txtHost.getText().trim();
        String portTxt = txtPort.getText().trim();
        String db = txtDb.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();

        if (host.isEmpty() || portTxt.isEmpty() || db.isEmpty() || user.isEmpty()) {
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#ff4e8e"));
                errorMsg.setText("Completa host/puerto/bd/usuario (contraseña si aplica) y vuelve a intentar");
            });
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portTxt);
        } catch (NumberFormatException e) {
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#ff4e8e"));
                errorMsg.setText("Puerto inválido");
            });
            return;
        }

        IDBAdapter adapter;
        try {
            adapter = DBAdapterFactory.adapter(selected);
        } catch (Exception e) {
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#ff4e8e"));
                errorMsg.setText("No se pudo crear adapter: " + e.getMessage());
            });
            return;
        }

        final ConnectionConfig cfg = adapter.toConnectionConfig(host, port, db, user, pass);
        final String queriesResource = adapter.queriesResource();

        try {
            DBComponent component = new DBComponent(
                    cfg.driverClassName(),
                    cfg.url(),
                    cfg.user(),
                    cfg.password(),
                    queriesResource
            );
            // Intentar ejecutar una query predefinida para verificar conexión
            try {
                component.query(new dbcomponent.DBQueryId("usuario.selectOne"));
                DBComponentRegistry.putReplacing(selected, component);
                refrescarIndicadoresConexion();
                Platform.runLater(() -> {
                    errorMsg.setTextFill(Color.web("#7CFC00"));
                    errorMsg.setText("Conectado correctamente a " + selected + "\n" + cfg.url());
                });
            } catch (Exception pingEx) {
                refrescarIndicadoresConexion();
                Platform.runLater(() -> {
                    errorMsg.setTextFill(Color.web("#ff4e8e"));
                    errorMsg.setText("Error conectando (ping): " + pingEx.getMessage());
                });
            }
        } catch (Exception e) {
            refrescarIndicadoresConexion();
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#ff4e8e"));
                errorMsg.setText("Error conectando: " + e.getMessage());
            });
        }
    }

    private void refrescarIndicadoresConexion() {
        Platform.runLater(() -> {
            boolean pg = DBComponentRegistry.isConnected(DatabaseType.POSTGRES);
            boolean my = DBComponentRegistry.isConnected(DatabaseType.MYSQL);

            estadoPostgres.setText("PostgreSQL: " + (pg ? "conectado" : "desconectado"));
            estadoPostgres.setTextFill(pg ? Color.web("#7CFC00") : Color.web("#ff4e8e"));

            estadoMySql.setText("MySQL: " + (my ? "conectado" : "desconectado"));
            estadoMySql.setTextFill(my ? Color.web("#7CFC00") : Color.web("#ff4e8e"));

            actualizarEstadoSimular();
        });
    }

    private DatabaseType getDatabaseTypeSeleccionada() {
        if (rbPostgres != null && rbPostgres.isSelected()) return DatabaseType.POSTGRES;
        if (rbMySql != null && rbMySql.isSelected()) return DatabaseType.MYSQL;
        return null;
    }

    private void actualizarEstadoSimular() {
        Platform.runLater(() -> {
            if (btnSimular == null) return;
            DatabaseType selected = getDatabaseTypeSeleccionada();
            boolean habilitado = selected != null && DBComponentRegistry.isConnected(selected);
            btnSimular.setDisable(!habilitado);
        });
    }

    private void rellenarDefaultsConexion(DatabaseType type) {
        if (type == null) return;

        // Al cambiar de motor, limpiamos datos de la BD anterior para evitar conexiones inválidas.
        txtHost.setText("localhost");
        txtDb.clear();
        txtUser.clear();
        txtPass.clear();

        if (type == DatabaseType.POSTGRES) {
            txtPort.setText("5432");
            txtDb.setPromptText("base de datos (PostgreSQL)");
            txtUser.setPromptText("usuario (ej. postgres)");
            errorMsg.setText("PostgreSQL seleccionado: ingresa credenciales y presiona Conectar");
        } else {
            txtPort.setText("3306");
            txtDb.setPromptText("base de datos (MySQL)");
            txtUser.setPromptText("usuario (MySQL)");
            errorMsg.setText("MySQL seleccionado: ingresa credenciales y presiona Conectar");
        }
        txtPass.setPromptText("contraseña");
        errorMsg.setTextFill(Color.web("#b6aaff"));
    }

    private void limpiarConexionActual() {
        DatabaseType selected = getDatabaseTypeSeleccionada();
        if (selected == null) {
            errorMsg.setTextFill(Color.web("#ff4e8e"));
            errorMsg.setText("Selecciona PostgreSQL o MySQL para limpiar su conexión");
            return;
        }

        DBComponentRegistry.clear(selected);
        refrescarIndicadoresConexion();
        actualizarEstadoSimular();

        errorMsg.setTextFill(Color.web("#b6aaff"));
        errorMsg.setText("Conexión limpiada para " + selected + ". Puedes ingresar nuevos datos y reconectar.");
    }

    private void ejecutarSimulacion() {
        // Validar DB seleccionada
        DatabaseType selected = getDatabaseTypeSeleccionada();
        errorMsg.setText("");
        if (selected == null) {
            errorMsg.setText("Selecciona PostgreSQL o MySQL antes de simular");
            return;
        }

        // Configurar DB para Cliente
        Cliente.setDatabaseType(selected);

        // Debe existir conexión previa
        if (!DBComponentRegistry.isConnected(selected)) {
            errorMsg.setText("Primero conecta a " + selected + " (ingresa datos y presiona Conectar)");
            return;
        }

        // Reset UI
        Platform.runLater(() -> {
            statsSinPool.setText("0% Éxito");
            statsConPool.setText("0% Éxito");
            targetProgresoSin.set(0);
            targetProgresoCon.set(0);
            panelGrafica.getChildren().clear();
        });

        new Thread(() -> {
            Cliente.activarFreno(false);

            int num;
            try {
                num = Integer.parseInt(txtPeticiones.getText());
            } catch (NumberFormatException ex) {
                Platform.runLater(() -> statsSinPool.setText("Número inválido"));
                return;
            }
            if (num <= 0) {
                Platform.runLater(() -> statsSinPool.setText("Ingresa un número mayor a 0"));
                return;
            }

            // ================= SIN POOL =================
            var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerSin = new EstadisticaManager(colaSin);
            var hiloSin = new Thread(managerSin);
            var cliente = new Cliente(num);
            cliente.setEstadisticaQueue(colaSin);

            final CountDownLatch terminadoSinPool = new CountDownLatch(1);
            final ScheduledFuture<?>[] futureSin = new ScheduledFuture<?>[1];
            futureSin[0] = scheduler.scheduleAtFixedRate(() -> {
                int completadas = cliente.getCompletadas();
                double progreso = completadas / (double) num;
                Platform.runLater(() -> {
                    targetProgresoSin.set(Math.min(progreso, 1.0));
                    statsSinPool.setText("Sin pool: " + completadas + "/" + num + " | faltan " + Math.max(0, num - completadas));
                });
                if (completadas >= num || Cliente.estaFrenado()) {
                    Platform.runLater(() -> targetProgresoSin.set(1.0));
                    if (futureSin[0] != null) futureSin[0].cancel(false);
                    if (terminadoSinPool.getCount() > 0) terminadoSinPool.countDown();
                }
            }, 0, 20, TimeUnit.MILLISECONDS);

            hiloSin.start();
            cliente.ejecutarSinPoolConEstadisticas();
            managerSin.stop();
            try { hiloSin.join(); } catch (InterruptedException ignored) {}
            try { terminadoSinPool.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            // Mostrar KPIs sin pool (éxitos/fallos + %)
            Platform.runLater(() -> {
                int ex = managerSin.getExitosas();
                int fa = managerSin.getFallidas();
                double exitoPct = managerSin.getPorcentajeExito();
                statsSinPool.setText(String.format("Sin pool: %d ok / %d fail | %.2f%% éxito", ex, fa, exitoPct));
            });

            // ================= CON POOL =================
            Cliente.activarFreno(false);
            var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerCon = new EstadisticaManager(colaCon);
            var hiloCon = new Thread(managerCon);
            var clientePool = new Cliente(num);
            clientePool.setEstadisticaQueue(colaCon);

            final CountDownLatch terminadoConPool = new CountDownLatch(1);
            final ScheduledFuture<?>[] futureCon = new ScheduledFuture<?>[1];
            futureCon[0] = scheduler.scheduleAtFixedRate(() -> {
                int completadas = clientePool.getCompletadas();
                double progreso = completadas / (double) num;
                Platform.runLater(() -> {
                    targetProgresoCon.set(Math.min(progreso, 1.0));
                    statsConPool.setText("Con pool: " + completadas + "/" + num + " | faltan " + Math.max(0, num - completadas));
                });
                if (completadas >= num || Cliente.estaFrenado()) {
                    Platform.runLater(() -> targetProgresoCon.set(1.0));
                    if (futureCon[0] != null) futureCon[0].cancel(false);
                    if (terminadoConPool.getCount() > 0) terminadoConPool.countDown();
                }
            }, 0, 20, TimeUnit.MILLISECONDS);

            hiloCon.start();
            clientePool.ejecutarConPoolConEstadisticas();
            managerCon.stop();
            try { hiloCon.join(); } catch (InterruptedException ignored) {}
            try { terminadoConPool.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            Platform.runLater(() -> {
                int ex = managerCon.getExitosas();
                int fa = managerCon.getFallidas();
                double exitoPct = managerCon.getPorcentajeExito();
                statsConPool.setText(String.format("Con pool: %d ok / %d fail | %.2f%% éxito", ex, fa, exitoPct));
            });

            // Gráficas
            Platform.runLater(() -> {
                mostrarGraficaEstadisticas(
                    managerSin.getExitosas(),
                    managerSin.getFallidas(),
                    managerCon.getExitosas(),
                    managerCon.getFallidas()
                );
            });
        }).start();
    }

    private void mostrarGraficaEstadisticas(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        panelGrafica.getChildren().clear();
        VBox grafica = new GraficaEstadisticas(exitosasSinPool, fallidasSinPool, exitosasConPool, fallidasConPool).crearGrafica();
        panelGrafica.getChildren().add(grafica);
    }
}
