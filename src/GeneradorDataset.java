import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class GeneradorDataset {

    public static void main(String[] args) {
        System.out.println("=== GENERADOR DE DATASETS DE PRUEBA PARA MOTOR AVL ===");
        
        // Dataset 1: Pequeño (Pruebas de trazabilidad manual)
        generarCSV("estudiantes_pequeno.csv", 20);
        
        // Dataset 2: Mediano (Validación de rendimiento asintótico O(log n))
        generarCSV("estudiantes_mediano.csv", 500);
        
        System.out.println("Generación finalizada. Archivos listos para el inicio del motor.");
    }

    private static void generarCSV(String nombreArchivo, int cantidadRegistros) {
        String[] nombresBase = {"Carlos", "Andres", "Juanse", "Maria", "Diana", "Laura", "Pedro", "Santiago"};
        Random random = new Random(42); // Semilla fija para consistencia en la réplica de pruebas

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo))) {
            for (int i = 0; i < cantidadRegistros; i++) {
                // Generación de IDs dispersos no consecutivos para validar el rebalanceo AVL
                int id = 1000 + random.nextInt(9000); 
                String nombre = nombresBase[random.nextInt(nombresBase.length)] + "_" + i;
                // Nota real entre 0.0 y 5.0 acotada a un decimal
                double nota = 1.0 + (4.0 * random.nextDouble());
                nota = Math.round(nota * 10.0) / 10.0;

                writer.write(id + "," + nombre + "," + nota);
                writer.newLine();
            }
            System.out.println("Archivo '" + nombreArchivo + "' creado con " + cantidadRegistros + " registros.");
        } catch (IOException e) {
            System.out.println("ERROR al generar el archivo " + nombreArchivo + ": " + e.getMessage());
        }
    }
}