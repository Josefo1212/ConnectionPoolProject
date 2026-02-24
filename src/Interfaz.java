import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Interfaz extends Application {
    @Override
    public void start(Stage stage) {
        var btnSimular = new Button("Iniciar simulación");
        var btnFreno = new Button("Freno de emergencia");
        var lblPeticiones = new Label("Número de peticiones:");
        var txtPeticiones = new TextField();
        var areaQueries = new TextArea();
        var statsBox = new VBox(10);
        statsBox.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10;");
        var statsLabel = new Label("Estadísticas");
        statsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #b96eff; -fx-font-weight: bold;");
        var statsSinPool = new Label();
        var statsConPool = new Label();
        statsSinPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        statsConPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        statsBox.getChildren().addAll(statsLabel, statsSinPool, statsConPool);

        areaQueries.setEditable(false);
        areaQueries.setPrefHeight(400);
        areaQueries.setPrefWidth(600);
        areaQueries.setStyle("-fx-control-inner-background: #222; -fx-text-fill: #b96eff; -fx-font-size: 13px; -fx-border-color: #b96eff; -fx-border-radius: 8; -fx-background-radius: 8;");
        areaQueries.setPromptText("Aquí se mostrarán las queries ejecutadas...");

        var layout = new VBox(15, lblPeticiones, txtPeticiones, btnSimular, btnFreno, areaQueries, statsBox);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #181028; -fx-border-radius: 12; -fx-background-radius: 12;");

        var scene = new Scene(layout, 650, 600);
        stage.setScene(scene);
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();

        btnSimular.setOnAction(_ -> {
            areaQueries.appendText("\n--- Simulación SIN pool de conexiones ---\n");
            int num;
            try {
                num = Integer.parseInt(txtPeticiones.getText());
            } catch (NumberFormatException ex) {
                areaQueries.appendText("Número inválido\n");
                return;
            }
            var cliente = new Cliente(num);
            cliente.setOutput(areaQueries);
            new Thread(() -> {
                var statsSin = cliente.ejecutarSinPoolConEstadisticas();
                Platform.runLater(() -> {
                    statsSinPool.setText(
                        "Sin pool: " + statsSin.exitosas + " exitosas, " + statsSin.fallidas + " fallidas, " +
                        String.format("%.2f", statsSin.porcentajeExito()) + "% éxito, " +
                        String.format("%.2f", statsSin.porcentajeFallo()) + "% fallo"
                    );
                });
                areaQueries.appendText("\n--- Simulación CON pool de conexiones ---\n");
                var clientePool = new Cliente(num);
                clientePool.setOutput(areaQueries);
                var statsCon = clientePool.ejecutarConPoolConEstadisticas();
                Platform.runLater(() -> {
                    statsConPool.setText(
                        "Con pool: " + statsCon.exitosas + " exitosas, " + statsCon.fallidas + " fallidas, " +
                        String.format("%.2f", statsCon.porcentajeExito()) + "% éxito, " +
                        String.format("%.2f", statsCon.porcentajeFallo()) + "% fallo"
                    );
                });
            }).start();
        });

        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno();
            areaQueries.appendText("Freno de emergencia activado\n");
        });
    }
}
