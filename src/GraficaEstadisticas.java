import javafx.scene.chart.PieChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Node;

public class GraficaEstadisticas {
    private final int exitosasSinPool;
    private final int fallidasSinPool;
    private final int exitosasConPool;
    private final int fallidasConPool;

    public GraficaEstadisticas(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        this.exitosasSinPool = exitosasSinPool;
        this.fallidasSinPool = fallidasSinPool;
        this.exitosasConPool = exitosasConPool;
        this.fallidasConPool = fallidasConPool;
    }

    public VBox crearGrafica() {
        VBox contenedor = new VBox(20);
        contenedor.setAlignment(Pos.CENTER);

        // Gráfico de torta para sin pool
        PieChart pieSinPool = new PieChart();
        pieSinPool.setTitle("Sin pool de conexiones");
        PieChart.Data dataExitosasSin = new PieChart.Data("Exitosas", exitosasSinPool);
        PieChart.Data dataFallidasSin = new PieChart.Data("Fallidas", fallidasSinPool);
        pieSinPool.getData().addAll(dataExitosasSin, dataFallidasSin);
        pieSinPool.setLabelsVisible(true);
        pieSinPool.setLegendVisible(true);
        pieSinPool.setPrefSize(160, 160);
        pieSinPool.setMaxSize(160, 160);
        pieSinPool.setMinSize(160, 160);
        pieSinPool.setStyle("-fx-pie-label-visible: true; -fx-font-size: 15px;");

        // Gráfico de torta para con pool
        PieChart pieConPool = new PieChart();
        pieConPool.setTitle("Con pool de conexiones");
        PieChart.Data dataExitosasCon = new PieChart.Data("Exitosas", exitosasConPool);
        PieChart.Data dataFallidasCon = new PieChart.Data("Fallidas", fallidasConPool);
        pieConPool.getData().addAll(dataExitosasCon, dataFallidasCon);
        pieConPool.setLabelsVisible(true);
        pieConPool.setLegendVisible(true);
        pieConPool.setPrefSize(160, 160);
        pieConPool.setMaxSize(160, 160);
        pieConPool.setMinSize(160, 160);
        pieConPool.setStyle("-fx-pie-label-visible: true; -fx-font-size: 15px;");

        // Forzar colores de las porciones
        pieSinPool.getData().forEach(data -> {
            Node node = data.getNode();
            if (node != null) {
                if ("Exitosas".equals(data.getName())) node.setStyle("-fx-pie-color: #2196f3;");
                if ("Fallidas".equals(data.getName())) node.setStyle("-fx-pie-color: #e53935;");
            }
        });
        pieConPool.getData().forEach(data -> {
            Node node = data.getNode();
            if (node != null) {
                if ("Exitosas".equals(data.getName())) node.setStyle("-fx-pie-color: #2196f3;");
                if ("Fallidas".equals(data.getName())) node.setStyle("-fx-pie-color: #e53935;");
            }
        });
        // Asegurar que los colores se apliquen después de renderizar
        pieSinPool.getData().forEach(data -> data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                if ("Exitosas".equals(data.getName())) newNode.setStyle("-fx-pie-color: #2196f3;");
                if ("Fallidas".equals(data.getName())) newNode.setStyle("-fx-pie-color: #e53935;");
            }
        }));
        pieConPool.getData().forEach(data -> data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                if ("Exitosas".equals(data.getName())) newNode.setStyle("-fx-pie-color: #2196f3;");
                if ("Fallidas".equals(data.getName())) newNode.setStyle("-fx-pie-color: #e53935;");
            }
        }));

        HBox hBox = new HBox(60, pieSinPool, pieConPool);
        hBox.setAlignment(Pos.CENTER);

        Text titulo = new Text("Resultados de la simulación");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titulo.setFill(Color.web("#6c2eb7"));
        titulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        contenedor.getChildren().addAll(titulo, hBox);
        return contenedor;
    }
}
