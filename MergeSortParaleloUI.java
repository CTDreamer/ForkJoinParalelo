import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MergeSortParaleloUI extends Application {

    // Modelo de datos
    private int[] datosOriginales;
    private int[] arregloParalelo;
    private int[] arregloSecuencial;
    private long tiempoParalelo;
    private long tiempoSecuencial;
    private int tamañoArreglo = 10_000_000; // Valor por defecto
    private int umbral = 1_000_000; // Valor por defecto
    
    // Propiedades para binding
    private final SimpleDoubleProperty progresoParalelo = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty progresoSecuencial = new SimpleDoubleProperty(0);
    private final SimpleBooleanProperty ordenando = new SimpleBooleanProperty(false);
    private final SimpleStringProperty estadoActual = new SimpleStringProperty("Listo para ordenar");
    
    // Componentes UI
    private ProgressBar barraProgresoParalelo;
    private ProgressBar barraProgresoSecuencial;
    private Button btnIniciar;
    private Slider sliderTamaño;
    private Slider sliderUmbral;
    private BarChart<String, Number> graficaTiempos;
    private TextArea consola;
    private Label lblResultado;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🚀 MergeSort Paralelo vs Secuencial");
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f5ff;");
        
        // Panel superior
        VBox panelSuperior = crearPanelSuperior();
        root.setTop(panelSuperior);
        
        // Panel central
        HBox panelCentral = crearPanelCentral();
        root.setCenter(panelCentral);
        
        // Panel inferior
        VBox panelInferior = crearPanelInferior();
        root.setBottom(panelInferior);
        
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private VBox crearPanelSuperior() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(0, 0, 20, 0));
        panel.setAlignment(Pos.CENTER);
        
        // Título
        Text titulo = new Text("Comparativa de Algoritmos de Ordenamiento");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setFill(Color.valueOf("#2a4d69"));
        
        // Configuración del tamaño
        HBox panelTamaño = new HBox(15);
        panelTamaño.setAlignment(Pos.CENTER);
        
        Label lblTamaño = new Label("Tamaño del arreglo:");
        lblTamaño.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        sliderTamaño = new Slider(1_000_000, 100_000_000, tamañoArreglo);
        sliderTamaño.setPrefWidth(300);
        sliderTamaño.setShowTickLabels(true);
        sliderTamaño.setShowTickMarks(true);
        sliderTamaño.setMajorTickUnit(25_000_000);
        sliderTamaño.setBlockIncrement(5_000_000);
        
        // Formateador para mostrar millones
        sliderTamaño.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double n) {
                return (n / 1_000_000) + "M";
            }

            @Override
            public Double fromString(String s) {
                return Double.valueOf(s);
            }
        });
        
        TextField txtTamaño = new TextField(NumberFormat.getNumberInstance(Locale.US).format(tamañoArreglo));
        txtTamaño.setPrefWidth(120);
        txtTamaño.setEditable(false);
        
        // Actualizar el campo de texto cuando se mueve el slider
        sliderTamaño.valueProperty().addListener((obs, oldVal, newVal) -> {
            tamañoArreglo = newVal.intValue();
            txtTamaño.setText(NumberFormat.getNumberInstance(Locale.US).format(tamañoArreglo));
        });
        
        panelTamaño.getChildren().addAll(lblTamaño, sliderTamaño, txtTamaño);
        
        // Configuración del umbral
        HBox panelUmbral = new HBox(15);
        panelUmbral.setAlignment(Pos.CENTER);
        
        Label lblUmbral = new Label("Umbral secuencial:");
        lblUmbral.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        sliderUmbral = new Slider(10_000, 5_000_000, umbral);
        sliderUmbral.setPrefWidth(300);
        sliderUmbral.setShowTickLabels(true);
        sliderUmbral.setShowTickMarks(true);
        sliderUmbral.setMajorTickUnit(1_000_000);
        sliderUmbral.setBlockIncrement(100_000);
        
        // Formateador para mostrar miles/millones
        sliderUmbral.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double n) {
                if (n >= 1_000_000)
                    return (n / 1_000_000) + "M";
                return (n / 1_000) + "K";
            }

            @Override
            public Double fromString(String s) {
                return Double.valueOf(s);
            }
        });
        
        TextField txtUmbral = new TextField(NumberFormat.getNumberInstance(Locale.US).format(umbral));
        txtUmbral.setPrefWidth(120);
        txtUmbral.setEditable(false);
        
        // Actualizar el campo de texto cuando se mueve el slider
        sliderUmbral.valueProperty().addListener((obs, oldVal, newVal) -> {
            umbral = newVal.intValue();
            txtUmbral.setText(NumberFormat.getNumberInstance(Locale.US).format(umbral));
        });
        
        panelUmbral.getChildren().addAll(lblUmbral, sliderUmbral, txtUmbral);
        
        // Botón de inicio
        btnIniciar = new Button("▶️ Iniciar Ordenamiento");
        btnIniciar.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnIniciar.setPrefWidth(200);
        btnIniciar.setPrefHeight(40);
        btnIniciar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        btnIniciar.setOnAction(e -> iniciarOrdenamiento());
        
        // Inhabilitar el botón mientras está ordenando
        btnIniciar.disableProperty().bind(ordenando);
        
        panel.getChildren().addAll(titulo, panelTamaño, panelUmbral, btnIniciar);
        return panel;
    }
    
    private HBox crearPanelCentral() {
        HBox panel = new HBox(20);
        panel.setAlignment(Pos.CENTER);
        
        // Panel izquierdo: Gráfica
        VBox panelGrafica = new VBox(10);
        panelGrafica.setAlignment(Pos.CENTER);
        panelGrafica.setPrefWidth(450);
        
        Label lblGrafica = new Label("⏱️ Comparativa de Tiempos (ms)");
        lblGrafica.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        
        graficaTiempos = new BarChart<>(xAxis, yAxis);
        graficaTiempos.setTitle("");
        xAxis.setLabel("Algoritmo");
        yAxis.setLabel("Tiempo (ms)");
        graficaTiempos.setLegendVisible(false);
        graficaTiempos.setAnimated(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Paralelo", 0));
        series.getData().add(new XYChart.Data<>("Secuencial", 0));
        
        graficaTiempos.getData().add(series);
        
        panelGrafica.getChildren().addAll(lblGrafica, graficaTiempos);
        
        // Panel derecho: Progreso y consola
        VBox panelProgreso = new VBox(15);
        panelProgreso.setAlignment(Pos.TOP_CENTER);
        panelProgreso.setPrefWidth(400);
        
        Label lblParalelo = new Label("🔄 Proceso Paralelo");
        lblParalelo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        barraProgresoParalelo = new ProgressBar(0);
        barraProgresoParalelo.setPrefWidth(380);
        barraProgresoParalelo.setPrefHeight(25);
        barraProgresoParalelo.progressProperty().bind(progresoParalelo);
        barraProgresoParalelo.setStyle("-fx-accent: #1E88E5;");
        
        Label lblSecuencial = new Label("🔄 Proceso Secuencial");
        lblSecuencial.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        barraProgresoSecuencial = new ProgressBar(0);
        barraProgresoSecuencial.setPrefWidth(380);
        barraProgresoSecuencial.setPrefHeight(25);
        barraProgresoSecuencial.progressProperty().bind(progresoSecuencial);
        barraProgresoSecuencial.setStyle("-fx-accent: #FFA000;");
        
        // Etiqueta que muestra el estado actual
        Label lblEstado = new Label();
        lblEstado.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblEstado.textProperty().bind(estadoActual);
        
        consola = new TextArea();
        consola.setPrefHeight(300);
        consola.setEditable(false);
        consola.setFont(Font.font("Monospaced", 12));
        
        panelProgreso.getChildren().addAll(lblParalelo, barraProgresoParalelo, 
                                          lblSecuencial, barraProgresoSecuencial, 
                                          lblEstado, consola);
        
        panel.getChildren().addAll(panelGrafica, panelProgreso);
        return panel;
    }
    
    private VBox crearPanelInferior() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20, 0, 0, 0));
        panel.setAlignment(Pos.CENTER);
        
        lblResultado = new Label("✅ Esperando resultados...");
        lblResultado.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        HBox infoPanel = new HBox(40);
        infoPanel.setAlignment(Pos.CENTER);
        
        // Información del sistema
        VBox sysInfo = new VBox(5);
        sysInfo.setAlignment(Pos.CENTER);
        
        Label lblCores = new Label("🧠 Núcleos disponibles: " + Runtime.getRuntime().availableProcessors());
        lblCores.setFont(Font.font("System", 14));
        
        Label lblMem = new Label("💾 Memoria disponible: " + 
                          (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        lblMem.setFont(Font.font("System", 14));
        
        sysInfo.getChildren().addAll(lblCores, lblMem);
        
        infoPanel.getChildren().add(sysInfo);
        panel.getChildren().addAll(lblResultado, infoPanel);
        
        return panel;
    }
    
    private void iniciarOrdenamiento() {
        ordenando.set(true);
        progresoParalelo.set(0);
        progresoSecuencial.set(0);
        consola.clear();
        
        // Crear tarea para ejecutar en segundo plano
        Task<Void> tareaOrdenamiento = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Paso 1: Generar datos
                updateProgress("🔄 Generando arreglo aleatorio de " + NumberFormat.getNumberInstance(Locale.US).format(tamañoArreglo) + " elementos...");
                datosOriginales = generarArregloAleatorio(tamañoArreglo);
                
                updateProgress("✅ Arreglo generado correctamente!");
                updateProgress("📊 Muestra de datos originales:");
                Platform.runLater(() -> imprimirResumen(datosOriginales));
                
                // Paso 2: Crear copias para cada algoritmo
                updateProgress("🔄 Preparando arreglos para ordenamiento...");
                arregloParalelo = Arrays.copyOf(datosOriginales, datosOriginales.length);
                arregloSecuencial = Arrays.copyOf(datosOriginales, datosOriginales.length);
                updateProgress("✅ Preparación completada!");
                
                // Paso 3: Ejecutar ordenamiento paralelo
                updateProgress("\n🚀 Iniciando ordenamiento PARALELO...");
                progresoParalelo.set(0.1); // Indicar inicio
                
                long inicioParalelo = System.nanoTime();
                ForkJoinPool pool = new ForkJoinPool();
                MergeSortParalelo tarea = new MergeSortParalelo(arregloParalelo, 0, arregloParalelo.length, umbral);
                pool.invoke(tarea);
                long finParalelo = System.nanoTime();
                
                tiempoParalelo = (finParalelo - inicioParalelo) / 1_000_000;
                progresoParalelo.set(1.0);
                updateProgress("✅ Ordenamiento paralelo completado en " + tiempoParalelo + " ms");
                
                // Paso 4: Ejecutar ordenamiento secuencial
                updateProgress("\n🔄 Iniciando ordenamiento SECUENCIAL...");
                progresoSecuencial.set(0.1); // Indicar inicio
                
                long inicioSecuencial = System.nanoTime();
                Arrays.sort(arregloSecuencial);
                long finSecuencial = System.nanoTime();
                
                tiempoSecuencial = (finSecuencial - inicioSecuencial) / 1_000_000;
                progresoSecuencial.set(1.0);
                updateProgress("✅ Ordenamiento secuencial completado en " + tiempoSecuencial + " ms");
                
                // Paso 5: Verificar resultados
                boolean resultadosIguales = Arrays.equals(arregloParalelo, arregloSecuencial);
                
                updateProgress("\n📊 Muestra de datos ordenados:");
                Platform.runLater(() -> imprimirResumen(arregloParalelo));
                
                // Paso 6: Actualizar UI con resultados finales
                Platform.runLater(() -> {
                    XYChart.Series<String, Number> nuevaSerie = new XYChart.Series<>();
                    nuevaSerie.getData().add(new XYChart.Data<>("Paralelo", tiempoParalelo));
                    nuevaSerie.getData().add(new XYChart.Data<>("Secuencial", tiempoSecuencial));
                    
                    graficaTiempos.getData().clear();
                    graficaTiempos.getData().add(nuevaSerie);
                    
                    // Aplicar colores diferentes a las barras
                    nuevaSerie.getData().get(0).getNode().setStyle("-fx-bar-fill: #1E88E5;");
                    nuevaSerie.getData().get(1).getNode().setStyle("-fx-bar-fill: #FFA000;");
                    
                    // Mostrar resultado de la comparación
                    if (resultadosIguales) {
                        if (tiempoParalelo < tiempoSecuencial) {
                            long diferencia = tiempoSecuencial - tiempoParalelo;
                            double speedup = (double) tiempoSecuencial / tiempoParalelo;
                            lblResultado.setText(String.format("✅ Paralelo fue más rápido por %d ms (%.2fx más rápido)", 
                                                          diferencia, speedup));
                            lblResultado.setTextFill(Color.GREEN);
                        } else {
                            long diferencia = tiempoParalelo - tiempoSecuencial;
                            lblResultado.setText("❓ Secuencial fue más rápido por " + diferencia + " ms");
                            lblResultado.setTextFill(Color.ORANGE);
                        }
                    } else {
                        lblResultado.setText("❌ ERROR: Los resultados no coinciden!");
                        lblResultado.setTextFill(Color.RED);
                    }
                });
                
                updateProgress("\n✅ Prueba completada! Los resultados son " + 
                               (resultadosIguales ? "iguales ✓" : "diferentes ✗"));
                
                updateProgress("\n⚡ Datos de rendimiento:");
                updateProgress("⏱ Tiempo de ejecución paralelo:   " + tiempoParalelo + " ms");
                updateProgress("⏱ Tiempo de ejecución secuencial: " + tiempoSecuencial + " ms");
                
                if (tiempoParalelo < tiempoSecuencial) {
                    double speedup = (double) tiempoSecuencial / tiempoParalelo;
                    updateProgress(String.format("📈 Aceleración (speedup): %.2fx", speedup));
                }
                
                return null;
            }
            
            private void updateProgress(String mensaje) {
                Platform.runLater(() -> {
                    consola.appendText(mensaje + "\n");
                    estadoActual.set(mensaje);
                });
            }
        };
        
        // Cuando la tarea termine, actualizar el estado UI
        tareaOrdenamiento.setOnSucceeded(e -> ordenando.set(false));
        tareaOrdenamiento.setOnFailed(e -> {
            ordenando.set(false);
            Throwable exc = tareaOrdenamiento.getException();
            consola.appendText("\n❌ ERROR: " + exc.getMessage() + "\n");
            exc.printStackTrace();
        });
        
        // Iniciar la tarea en un hilo separado
        Thread hiloTarea = new Thread(tareaOrdenamiento);
        hiloTarea.setDaemon(true);
        hiloTarea.start();
    }
    
    public void imprimirResumen(int[] arreglo) {
        StringBuilder sb = new StringBuilder();
        int mostrar = 10;
        
        sb.append("Inicio: ");
        for (int i = 0; i < mostrar && i < arreglo.length; i++) {
            sb.append(arreglo[i]).append(" ");
        }
        
        sb.append("\nFin:    ");
        for (int i = Math.max(0, arreglo.length - mostrar); i < arreglo.length; i++) {
            sb.append(arreglo[i]).append(" ");
        }
        
        consola.appendText(sb.toString() + "\n");
    }

    public static int[] generarArregloAleatorio(int tamaño) {
        int[] arreglo = new int[tamaño];
        for (int i = 0; i < tamaño; i++) {
            arreglo[i] = (int)(Math.random() * 1_000_000);
        }
        return arreglo;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    // Clase MergeSortParalelo adaptada para la UI
    public static class MergeSortParalelo extends RecursiveAction {
        private int[] arreglo;
        private int inicio, fin;
        private int umbral;

        public MergeSortParalelo(int[] arreglo, int inicio, int fin, int umbral) {
            this.arreglo = arreglo;
            this.inicio = inicio;
            this.fin = fin;
            this.umbral = umbral;
        }

        @Override
        protected void compute() {
            if ((fin - inicio) <= umbral) {
                Arrays.sort(arreglo, inicio, fin);
            } else {
                int medio = (inicio + fin) / 2;
                MergeSortParalelo izquierda = new MergeSortParalelo(arreglo, inicio, medio, umbral);
                MergeSortParalelo derecha = new MergeSortParalelo(arreglo, medio, fin, umbral);
                invokeAll(izquierda, derecha);
                merge(inicio, medio, fin);
            }
        }

        private void merge(int inicio, int medio, int fin) {
            int[] temp = new int[fin - inicio];
            int i = inicio, j = medio, k = 0;

            while (i < medio && j < fin) {
                temp[k++] = (arreglo[i] <= arreglo[j]) ? arreglo[i++] : arreglo[j++];
            }

            while (i < medio) temp[k++] = arreglo[i++];
            while (j < fin)   temp[k++] = arreglo[j++];

            for (int x = 0; x < temp.length; x++) {
                arreglo[inicio + x] = temp[x];
            }
        }
    }
}