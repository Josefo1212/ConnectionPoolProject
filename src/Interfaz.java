import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Interfaz extends Application {
    private final VBox panelGrafica = new VBox();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private DoubleProperty progresoSinPoolProp = new SimpleDoubleProperty(0);
    private DoubleProperty progresoConPoolProp = new SimpleDoubleProperty(0);
    @Override
    public void start(Stage stage) {
        // Título
        var titulo = new Label("Simulación de Pool de Conexiones");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#a88ff0"));
        titulo.setAlignment(Pos.CENTER);
        titulo.setPadding(new Insets(0, 0, 10, 0));
        // Campo de peticiones
        var lblPeticiones = new Label("Número de peticiones:");
        lblPeticiones.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblPeticiones.setTextFill(Color.web("#a88ff0"));
        var txtPeticiones = new TextField();
        txtPeticiones.setFont(Font.font("Segoe UI", 14));
        txtPeticiones.setPrefWidth(140);
        txtPeticiones.setStyle("-fx-background-color: #292a3a; -fx-text-fill: #e0e0f0; -fx-border-color: #a88ff0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 6 10 6 10;");
        var peticionesBox = new HBox(lblPeticiones, txtPeticiones);
        peticionesBox.setSpacing(10);
        peticionesBox.setAlignment(Pos.CENTER);
        // Botones
        var btnSimular = new Button("Iniciar simulación");
        btnSimular.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnSimular.setStyle("-fx-background-color: linear-gradient(to bottom, #a88ff0 0%, #7c6ee6 100%); -fx-text-fill: #23243a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 28 10 28;");
        var btnFreno = new Button("Freno de emergencia");
        btnFreno.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnFreno.setStyle("-fx-background-color: linear-gradient(to bottom, #ff4e8e 0%, #a88ff0 100%); -fx-text-fill: #23243a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 28 10 28;");
        var controls = new HBox(btnSimular, btnFreno);
        controls.setSpacing(15);
        controls.setAlignment(Pos.CENTER);
        // Estadísticas y progreso
        var statsLabel = new Label("Estadísticas");
        statsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        statsLabel.setTextFill(Color.web("#a88ff0"));
        statsLabel.setAlignment(Pos.CENTER);
        var statsSinPool = new Label();
        statsSinPool.setFont(Font.font("Segoe UI", 13));
        statsSinPool.setTextFill(Color.web("#e0e0f0"));
        statsSinPool.setAlignment(Pos.CENTER);
        var statsConPool = new Label();
        statsConPool.setFont(Font.font("Segoe UI", 13));
        statsConPool.setTextFill(Color.web("#e0e0f0"));
        statsConPool.setAlignment(Pos.CENTER);
        var progressBarSinPool = new ProgressBar();
        progressBarSinPool.setPrefWidth(600);
        progressBarSinPool.setPrefHeight(16);
        progressBarSinPool.progressProperty().bind(progresoSinPoolProp);
        progressBarSinPool.setStyle("-fx-progress-color: #a88ff0;");
        var progressBarConPool = new ProgressBar();
        progressBarConPool.setPrefWidth(600);
        progressBarConPool.setPrefHeight(16);
        progressBarConPool.progressProperty().bind(progresoConPoolProp);
        progressBarConPool.setStyle("-fx-progress-color: #a88ff0;");
        var progresoSinPool = new Label();
        progresoSinPool.setFont(Font.font("Segoe UI", 12));
        progresoSinPool.setTextFill(Color.web("#b6aaff"));
        progresoSinPool.setAlignment(Pos.CENTER);
        var progresoConPool = new Label();
        progresoConPool.setFont(Font.font("Segoe UI", 12));
        progresoConPool.setTextFill(Color.web("#b6aaff"));
        progresoConPool.setAlignment(Pos.CENTER);
        // Caja de estadísticas
        var statsBox = new VBox(
            statsLabel,
            progressBarSinPool, progresoSinPool, statsSinPool,
            progressBarConPool, progresoConPool, statsConPool
        );
        statsBox.setSpacing(5);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #23243a; -fx-border-radius: 14; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);");
        statsBox.setAlignment(Pos.CENTER);
        // Panel para la gráfica
        panelGrafica.setStyle("-fx-alignment: center;");
        panelGrafica.setMinHeight(180);
        panelGrafica.setMaxHeight(220);
        panelGrafica.setPrefHeight(200);
        panelGrafica.setVisible(false);
        // Leyenda de colores para gráficas
        var leyenda = new HBox();
        var exitoColor = new Label("  ");
        exitoColor.setStyle("-fx-background-color: #2196f3; -fx-min-width: 22px; -fx-min-height: 16px; -fx-max-width: 22px; -fx-max-height: 16px; -fx-border-radius: 6; -fx-background-radius: 6;");
        var exitoText = new Label("Exitosas");
        exitoText.setTextFill(Color.web("#2196f3"));
        var falloColor = new Label("  ");
        falloColor.setStyle("-fx-background-color: #e53935; -fx-min-width: 22px; -fx-min-height: 16px; -fx-max-width: 22px; -fx-max-height: 16px; -fx-border-radius: 6; -fx-background-radius: 6;");
        var falloText = new Label("Fallidas");
        falloText.setTextFill(Color.web("#e53935"));
        leyenda.setSpacing(12);
        leyenda.setAlignment(Pos.CENTER);
        leyenda.getChildren().addAll(exitoColor, exitoText, falloColor, falloText);
        // Ajuste de tamaño y disposición visual
        panelGrafica.setMinHeight(180);
        panelGrafica.setMaxHeight(220);
        panelGrafica.setPrefHeight(200);
        // Ajuste de tamaño de la ventana y elementos
        var mainBox = new VBox(
            titulo,
            peticionesBox,
            controls,
            statsBox,
            panelGrafica,
            leyenda
        );
        mainBox.setSpacing(10);
        mainBox.setPadding(new Insets(10, 8, 8, 8));
        mainBox.setAlignment(Pos.CENTER);
        var scene = new Scene(mainBox, 720, 520); // Ventana más baja y menos ancha
        // CSS embebido mejorado y suavizado
        var css = """
            .root {
                -fx-background-color: linear-gradient(to bottom, #23243a 0%, #2d2e4a 100%);
                -fx-font-family: 'Segoe UI', 'Arial', sans-serif;
            }
            .titulo {
                -fx-font-size: 24px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-padding: 0 0 10 0;
                -fx-alignment: center;
            }
            .label {
                -fx-font-size: 15px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-alignment: center-left;
            }
            .input {
                -fx-background-color: #292a3a;
                -fx-text-fill: #e0e0f0;
                -fx-border-color: #a88ff0;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-font-size: 14px;
                -fx-padding: 6 10 6 10;
            }
            .boton {
                -fx-background-color: linear-gradient(to bottom, #a88ff0 0%, #7c6ee6 100%);
                -fx-text-fill: #23243a;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-padding: 10 32 10 32;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
                -fx-alignment: center;
            }
            .boton:hover {
                -fx-background-color: linear-gradient(to bottom, #b6aaff 0%, #a88ff0 100%);
                -fx-text-fill: #23243a;
            }
            .area {
                -fx-control-inner-background: #23243a;
                -fx-text-fill: #a88ff0;
                -fx-font-size: 15px;
                -fx-border-color: #a88ff0;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
            }
            .stats-box {
                -fx-background-color: #23243a;
                -fx-padding: 20;
                -fx-border-radius: 16;
                -fx-background-radius: 16;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
                -fx-alignment: center;
            }
            .stats-label {
                -fx-font-size: 16px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-alignment: center;
            }
            .stats {
                -fx-text-fill: #e0e0f0;
                -fx-font-size: 13px;
                -fx-padding: 4 0 4 0;
                -fx-alignment: center;
            }
         """;
        try {
            java.nio.file.Path tempCss = java.nio.file.Files.createTempFile("estilo", ".css");
            java.nio.file.Files.writeString(tempCss, css);
            scene.getStylesheets().add(tempCss.toUri().toString());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el CSS: " + e.getMessage());
        }
        // Acciones de los botones y lógica de simulación
        btnSimular.setOnAction(_ -> {
            Platform.runLater(() -> {
                statsSinPool.setText("");
                statsConPool.setText("");
                progresoSinPool.setText("");
                progresoConPool.setText("");
                progresoSinPoolProp.set(0);
                progresoConPoolProp.set(0);
                panelGrafica.getChildren().clear();
                panelGrafica.setVisible(false);
                statsBox.setVisible(true);
            });
            new Thread(() -> {
                Cliente.activarFreno(false);
                int num;
                try { num = Integer.parseInt(txtPeticiones.getText()); }
                catch (NumberFormatException ex) {
                    Platform.runLater(() -> statsSinPool.setText("Número inválido"));
                    return;
                }
                var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerSin = new EstadisticaManager(colaSin);
                var hiloSin = new Thread(managerSin);
                var cliente = new Cliente(num);
                cliente.setEstadisticaQueue(colaSin);
                final boolean[] terminadoSinPool = {false};
                final ScheduledFuture<?>[] futureSin = new ScheduledFuture<?>[1];
                futureSin[0] = scheduler.scheduleAtFixedRate(() -> {
                    int completadas = cliente.getCompletadas();
                    double progreso = completadas / (double) num;
                    Platform.runLater(() -> {
                        progresoSinPoolProp.set(Math.min(progreso, 1.0));
                        progresoSinPool.setText("Sin pool: " + completadas + " completadas | " + (num - completadas) + " faltantes");
                    });
                    if (completadas >= num || Cliente.estaFrenado()) {
                        Platform.runLater(() -> {
                            progresoSinPoolProp.set(1.0);
                            progresoSinPool.setText("Sin pool: " + cliente.getCompletadas() + " completadas | 0 faltantes");
                        });
                        terminadoSinPool[0] = true;
                        if (futureSin[0] != null) futureSin[0].cancel(false);
                    }
                }, 0, 40, TimeUnit.MILLISECONDS);
                hiloSin.start();
                cliente.ejecutarSinPoolConEstadisticas();
                managerSin.stop();
                try { hiloSin.join(); } catch (InterruptedException ignored) {}
                while (!terminadoSinPool[0]) {
                    try { Thread.sleep(40); } catch (InterruptedException ignored) {}
                }
                Platform.runLater(() -> {
                    int exitosas = managerSin.getExitosas();
                    int fallidas = managerSin.getFallidas();
                    double exitoPct = managerSin.getPorcentajeExito();
                    double falloPct = managerSin.getPorcentajeFallo();
                    statsSinPool.setText(
                        "✅ Sin pool: " + exitosas + " exitosas, " + fallidas + " fallidas, " +
                        String.format("%.2f", exitoPct) + "% éxito, " +
                        String.format("%.2f", falloPct) + "% fallo"
                    );
                    progresoSinPool.setText("");
                });
                Cliente.activarFreno(false);
                var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerCon = new EstadisticaManager(colaCon);
                var hiloCon = new Thread(managerCon);
                var clientePool = new Cliente(num);
                clientePool.setEstadisticaQueue(colaCon);
                final boolean[] terminadoConPool = {false};
                final ScheduledFuture<?>[] futureCon = new ScheduledFuture<?>[1];
                futureCon[0] = scheduler.scheduleAtFixedRate(() -> {
                    int completadas = clientePool.getCompletadas();
                    double progreso = completadas / (double) num;
                    Platform.runLater(() -> {
                        progresoConPoolProp.set(Math.min(progreso, 1.0));
                        progresoConPool.setText("Con pool: " + completadas + " completadas | " + (num - completadas) + " faltantes");
                    });
                    if (completadas >= num || Cliente.estaFrenado()) {
                        Platform.runLater(() -> {
                            progresoConPoolProp.set(1.0);
                            progresoConPool.setText("Con pool: " + clientePool.getCompletadas() + " completadas | 0 faltantes");
                        });
                        terminadoConPool[0] = true;
                        if (futureCon[0] != null) futureCon[0].cancel(false);
                    }
                }, 0, 40, TimeUnit.MILLISECONDS);
                hiloCon.start();
                clientePool.ejecutarConPoolConEstadisticas();
                managerCon.stop();
                try { hiloCon.join(); } catch (InterruptedException ignored) {}
                while (!terminadoConPool[0]) {
                    try { Thread.sleep(40); } catch (InterruptedException ignored) {}
                }
                Platform.runLater(() -> {
                    int exitosas = managerCon.getExitosas();
                    int fallidas = managerCon.getFallidas();
                    double exitoPct = managerCon.getPorcentajeExito();
                    double falloPct = managerCon.getPorcentajeFallo();
                    statsConPool.setText(
                        "✨ Con pool: " + exitosas + " exitosas, " + fallidas + " fallidas, " +
                        String.format("%.2f", exitoPct) + "% éxito, " +
                        String.format("%.2f", falloPct) + "% fallo"
                    );
                    progresoConPool.setText("");
                });
                Platform.runLater(() -> {
                    mostrarGraficaEstadisticas(
                        managerSin.getExitosas(),
                        managerSin.getFallidas(),
                        managerCon.getExitosas(),
                        managerCon.getFallidas()
                    );
                    panelGrafica.setVisible(true);
                });
            }).start();
        });
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            Platform.runLater(() -> statsSinPool.setText("Freno de emergencia activado"));
        });
        stage.setScene(scene);
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();
    }

    private void mostrarGraficaEstadisticas(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        panelGrafica.getChildren().clear();
        VBox grafica = new GraficaEstadisticas(exitosasSinPool, fallidasSinPool, exitosasConPool, fallidasConPool).crearGrafica();
        panelGrafica.getChildren().add(grafica);
    }
}
