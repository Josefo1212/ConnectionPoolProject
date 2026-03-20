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

public class Interfaz extends Application {
    private final VBox panelGrafica = new VBox();

    private final Label statsPool = new Label("0% Éxito");

    private final ProgressBar progressPool = new ProgressBar(0);

    private TextField txtPeticiones;
    private RadioButton rbPostgres, rbMySql;
    private Label estadoPostgres, estadoMySql;
    private Button btnSimular;

    // Campos conexión
    private TextField txtHost, txtPort, txtDb, txtUser;
    private PasswordField txtPass;

    private final ConexionService conexionService = new ConexionService();
    private final SimulacionService simulacionService = new SimulacionService();

    // Progreso objetivo (lo que reporta la simulación)
    private final DoubleProperty targetProgresoPool = new SimpleDoubleProperty(0);

    // Progreso mostrado (se interpola para verse fluido)
    private final DoubleProperty shownProgresoPool = new SimpleDoubleProperty(0);

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

        // Binding de progreso al valor suavizado para animación fluida.
        progressPool.progressProperty().bind(shownProgresoPool);

        // Tarjeta KPI principal (solo pool)
        HBox kpiBox = new HBox(15);
        VBox cardPool = crearTarjetaKPI("Simulación con Pool", statsPool, progressPool, "#00ffff");
        HBox.setHgrow(cardPool, Priority.ALWAYS);
        kpiBox.getChildren().add(cardPool);

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
            simulacionService.activarFreno(true);
            errorMsg.setText("");
            Platform.runLater(() -> statsPool.setText("Freno de emergencia activado"));
        });

        stage.setOnCloseRequest(_ -> {
            simulacionService.shutdown();
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
                smoothStep(shownProgresoPool, targetProgresoPool, ALPHA);
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

        try {
            // Asegura que no se reutilice un pool anterior para la misma BD/credenciales.
            ConexionService.ConnectionView result = conexionService.conectar(selected, host, port, db, user, pass);
            refrescarIndicadoresConexion();
            Platform.runLater(() -> {
                errorMsg.setTextFill(Color.web("#7CFC00"));
                errorMsg.setText("Conectado correctamente a " + selected + "\n" + result.url());
            });
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
            boolean pg = conexionService.isConnected(DatabaseType.POSTGRES);
            boolean my = conexionService.isConnected(DatabaseType.MYSQL);

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
            boolean habilitado = selected != null && conexionService.isConnected(selected);
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

        conexionService.limpiarConexion(selected);
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

        // Debe existir conexión previa
        if (!conexionService.isConnected(selected)) {
            errorMsg.setText("Primero conecta a " + selected + " (ingresa datos y presiona Conectar)");
            return;
        }

        // Reset UI
        Platform.runLater(() -> {
            statsPool.setText("0% Éxito");
            targetProgresoPool.set(0);
            panelGrafica.getChildren().clear();
        });

        int num;
        try {
            num = Integer.parseInt(txtPeticiones.getText());
        } catch (NumberFormatException ex) {
            statsPool.setText("Número inválido");
            return;
        }

        try {
            simulacionService.ejecutarConPool(
                    selected,
                    num,
                    progress -> Platform.runLater(() -> {
                        targetProgresoPool.set(progress.progreso());
                        statsPool.setText("Pool: " + progress.completadas() + "/" + progress.total() + " | faltan " + Math.max(0, progress.total() - progress.completadas()));
                    }),
                    resultado -> Platform.runLater(() -> {
                        statsPool.setText(String.format("Pool: %d ok / %d fail | %.2f%% éxito", resultado.exitosas(), resultado.fallidas(), resultado.porcentajeExito()));
                        mostrarGraficaEstadisticas(resultado.exitosas(), resultado.fallidas());
                    }),
                    info -> Platform.runLater(() -> errorMsg.setText(info))
            );
        } catch (Exception e) {
            errorMsg.setText(e.getMessage());
        }
    }

    private void mostrarGraficaEstadisticas(int exitosasPool, int fallidasPool) {
        panelGrafica.getChildren().clear();
        VBox grafica = new GraficaEstadisticas(exitosasPool, fallidasPool).crearGrafica();
        panelGrafica.getChildren().add(grafica);
    }
}
