import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {
    private Map<String, Tabla> tablas;

    public DatabaseManager() {
        this.tablas = new HashMap<>();
        verificarYGenerarDatasets();
    }

    public void crearTabla(String nombre) {
        if (!tablas.containsKey(nombre)) {
            Tabla nuevaTabla = new Tabla(nombre);
            tablas.put(nombre, nuevaTabla);
        } else {
            System.out.println("ERROR: La tabla '" + nombre + "' ya existe en memoria.");
        }
    }

    public void borrarTabla(String nombre) {
        tablas.remove(nombre);
    }

    public Tabla getTabla(String nombre) {
        return tablas.get(nombre);
    }

    public Set<String> getListaTablas() {
        return this.tablas.keySet();
    }

    /**
     * Evalúa la existencia de los datasets de prueba y los genera si es necesario.
     */
    private void verificarYGenerarDatasets() {
        String[] archivosPrueba = {"estudiantes_pequeno.csv", "estudiantes_mediano.csv"};
        int[] tamaños = {10, 300}; // Tamaño pequeño (10) y mediano (300)

        for (int i = 0; i < archivosPrueba.length; i++) {
            File archivo = new File(archivosPrueba[i]);
            
            // Solo se generan si el archivo físico no existe en la raíz
            if (!archivo.exists()) {
                generarDatasetSintetico(archivosPrueba[i], tamaños[i]);
            }
        }
    }

    /**
     * Escribe datos  en disco.
     */
    private void generarDatasetSintetico(String nombreArchivo, int cantidad) {
        String[] poolNombres = {"Andres", "Carlos", "Juanse", "Maria", "Diana", "Laura", "Pedro", "Sofia", "Diego", "Ana"};
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo))) {
            for (int i = 1; i <= cantidad; i++) {
                // Identificadores fijos secuenciales para evitar colisiones primarias
                int id = 2000 + i; 
                String nombre = poolNombres[i % poolNombres.length] + "_" + i;
                
                // Generación de nota pseudoaleatoria acotada a tres cifras significativas (Ej: 4.2)
                double nota = Math.round((Math.random() * 5.0) * 10.0) / 10.0;

                writer.write(id + "," + nombre + "," + nota);
                writer.newLine();
            }
            System.out.println(">> Sistema: Dataset automático '" + nombreArchivo + "' creado con éxito en disco (" + cantidad + " registros).");
        } catch (IOException e) {
            System.out.println("ERROR CRÍTICO inicializando dataset: " + e.getMessage());
        }
    }
}