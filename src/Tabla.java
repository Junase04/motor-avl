import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tabla {
    private String nombre;
    private AVLTree arbol;
    private int cantidadRegistros;

    public Tabla(String nombre) {
        this.nombre = nombre;
        this.arbol = new AVLTree();
        this.cantidadRegistros = 0;
        cargarDeDisco();
    }

    public String getNombre() {
        return this.nombre;
    }

    public int getCantidadRegistros() {
        return this.cantidadRegistros;
    }

    public AVLTree getArbol() {
        return this.arbol;
    }

    /**
     * Inserta un registro aplicando reversión automática en caso de fallo de persistencia.
     */
    public void insertar(int id, Row registro) throws IOException {
        AVLTree.Node backupRaiz = this.arbol.getRoot();
        int backupCantidad = this.cantidadRegistros;

        try {
            this.arbol.insertar(id, registro);
            this.cantidadRegistros++;
            guardarADisco(); // Intenta persistir inmediatamente
        } catch (IOException e) {
            // ROLLBACK: Restauración del estado previo en memoria RAM
            this.arbol.setRoot(backupRaiz);
            this.cantidadRegistros = backupCantidad;
            throw e; 
        }
    }

    /**
     * Remueve un registro aplicando reversión automática en caso de fallo de persistencia.
     */
    public void eliminarRegistro(int id) throws IOException {
        if (this.arbol.buscar(id) == null) {
            throw new IllegalArgumentException("El ID especificado (" + id + ") no existe en la tabla.");
        }

        AVLTree.Node backupRaiz = this.arbol.getRoot();
        int backupCantidad = this.cantidadRegistros;

        try {
            this.arbol.eliminar(id);
            this.cantidadRegistros--;
            guardarADisco();
        } catch (IOException e) {
            // ROLLBACK
            this.arbol.setRoot(backupRaiz);
            this.cantidadRegistros = backupCantidad;
            throw e;
        }
    }

    /**
     * Modifica un registro aplicando reversión automática en caso de fallo de persistencia.
     */
    public void actualizarRegistro(int id, String campo, Object nuevoValor) throws IOException {
        Row fila = this.arbol.buscar(id);
        if (fila == null) {
            throw new IllegalArgumentException("El ID especificado (" + id + ") no existe en la tabla.");
        }

        Object valorAnterior = fila.get(campo);
        AVLTree.Node backupRaiz = this.arbol.getRoot();

        try {
            fila.put(campo, nuevoValor);
            guardarADisco();
        } catch (IOException e) {
            // ROLLBACK: Devuelve el campo a su estado original y restaura punteros
            fila.put(campo, valorAnterior);
            this.arbol.setRoot(backupRaiz);
            throw e;
        }
    }

    public Row buscar(int id) {
        return this.arbol.buscar(id);
    }

    public void imprimirTabla() {
        this.arbol.inorder();
    }

    public List<String> buscarPorRango(String operador, int limiteId) {
        List<String> resultados = new ArrayList<>();
        recorridoRango(this.arbol.getRoot(), operador, limiteId, resultados);
        return resultados;
    }

    private void recorridoRango(AVLTree.Node nodo, String operador, int limiteId, List<String> resultados) {
        if (nodo == null) return;

        recorridoRango(nodo.left, operador, limiteId, resultados);

        boolean cumple = false;
        switch (operador) {
            case ">":  cumple = nodo.key > limiteId; break;
            case "<":  cumple = nodo.key < limiteId; break;
            case ">=": cumple = nodo.key >= limiteId; break;
            case "<=": cumple = nodo.key <= limiteId; break;
        }

        if (cumple) {
            resultados.add("  ID: " + nodo.key + " -> " + nodo.record.toString());
        }

        recorridoRango(nodo.right, operador, limiteId, resultados);
    }

    public void cargarDeDisco() {
        File archivo = new File(this.nombre + ".csv");
        if (!archivo.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] tokens = linea.split(",");
                if (tokens.length == 3) {
                    int id = Integer.parseInt(tokens[0].trim());
                    String nombreReg = tokens[1].trim();
                    double notaReg = Double.parseDouble(tokens[2].trim());

                    Row fila = new Row();
                    fila.put("nombre", nombreReg);
                    fila.put("nota", notaReg);

                    this.arbol.insertar(id, fila);
                    this.cantidadRegistros++;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("ERROR en la reconstrucción del índice desde disco: " + e.getMessage());
        }
    }

    public void guardarADisco() throws IOException {
        File archivo = new File(this.nombre + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            volcarNodo(this.arbol.getRoot(), writer);
        }
    }

    private void volcarNodo(AVLTree.Node nodo, BufferedWriter writer) throws IOException {
        if (nodo != null) {
            volcarNodo(nodo.left, writer);
            Object nombreObj = nodo.record.get("nombre");
            Object notaObj = nodo.record.get("nota");
            writer.write(nodo.key + "," + nombreObj + "," + notaObj);
            writer.newLine();
            volcarNodo(nodo.right, writer);
        }
    }
}