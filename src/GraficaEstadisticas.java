import javafx.scene.chart.PieChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.Node;

public class GraficaEstadisticas {
    private final int exitosasSinPool, fallidasSinPool, exitosasConPool, fallidasConPool;

    public GraficaEstadisticas(int es, int fs, int ec, int fc) {
        this.exitosasSinPool = es;
        this.fallidasSinPool = fs;
        this.exitosasConPool = ec;
        this.fallidasConPool = fc;
    }

    public VBox crearGrafica() {
        HBox contenedorGraficas = new HBox(40);
        contenedorGraficas.setAlignment(Pos.CENTER);

        PieChart pcSin = crearPie("Sin Pool", exitosasSinPool, fallidasSinPool);
        PieChart pcCon = crearPie("Con Pool", exitosasConPool, fallidasConPool);

        HBox.setHgrow(pcSin, Priority.ALWAYS);
        HBox.setHgrow(pcCon, Priority.ALWAYS);

        contenedorGraficas.getChildren().addAll(pcSin, pcCon);

        VBox layout = new VBox(10, contenedorGraficas);
        layout.setAlignment(Pos.CENTER);
        return layout;
    }

    private PieChart crearPie(String titulo, int exito, int fallo) {
        PieChart pie = new PieChart();
        pie.setTitle(titulo);

        PieChart.Data d0 = new PieChart.Data("Exitosas", exito);
        PieChart.Data d1 = new PieChart.Data("Fallidas", fallo);
        pie.getData().addAll(d0, d1);

        pie.setLabelsVisible(true);
        pie.setLegendVisible(false);

        // Colores: azul (éxito) / rojo (fallo)
        applySliceColor(d0, "#2196f3");
        applySliceColor(d1, "#e53935");

        return pie;
    }

    private void applySliceColor(PieChart.Data data, String color) {
        Node n = data.getNode();
        if (n != null) {
            n.setStyle("-fx-pie-color: " + color + ";");
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-pie-color: " + color + ";");
            }
        });
    }
}
