import java.io.File;
import java.util.Scanner;

public class ConsoleInterface {
    private DatabaseManager dbManager;
    private boolean running;

    public ConsoleInterface(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.running = true;
    }

    /**
     * Arranca el bucle de ejecución principal de la terminal (REPL).
     */
    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("MOTOR DE BASE DE DATOS AVL - INTERFAZ DE LÍNEA DE COMANDOS");
        System.out.println("Escriba 'HELP' para ver los comandos disponibles o 'EXIT' para salir.");
        System.out.println("------------------------------------------------------------------");

        while (running) {
            System.out.print("db_engine> ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                procesarComando(input);
            }
        }
        scanner.close();
    }

    /**
     * Analizador léxico-sintáctico primario.
     */
    private void procesarComando(String input) {
        String[] tokens = input.split("\\s+");
        String comandoPrincipal = tokens[0].toUpperCase();

        try {
            switch (comandoPrincipal) {
                case "CREATE":
                    if (tokens.length < 3 || !tokens[1].toUpperCase().equals("TABLE")) {
                        System.out.println("ERROR: Sintaxis inválida. Uso correcto: CREATE TABLE <nombre_tabla>");
                        return;
                    }
                    dbManager.crearTabla(tokens[2]);
                    break;

                case "DROP":
                    if (tokens.length < 3 || !tokens[1].toUpperCase().equals("TABLE")) {
                        System.out.println("ERROR: Sintaxis inválida. Uso correcto: DROP TABLE <nombre_tabla>");
                        return;
                    }
                    ejecutarDrop(tokens[2]);
                    break;

                case "INSERT":
                    ejecutarInsert(tokens, input);
                    break;

                case "SELECT":
                    ejecutarSelect(tokens);
                    break;

                case "PRINT":
                    if (tokens.length < 2) {
                        System.out.println("ERROR: Sintaxis inválida. Uso correcto: PRINT <nombre_tabla>");
                        return;
                    }
                    Tabla tPrint = dbManager.getTabla(tokens[1]);
                    if (tPrint != null) {
                        System.out.println("Registros de la tabla '" + tokens[1] + "':");
                        tPrint.imprimirTabla();
                    } else {
                        System.out.println("ERROR: La tabla '" + tokens[1] + "' no existe.");
                    }
                    break;

                case "SAVE":
                    if (tokens.length < 2) {
                        System.out.println("ERROR: Sintaxis inválida. Uso correcto: SAVE <nombre_tabla>");
                        return;
                    }
                    Tabla tSave = dbManager.getTabla(tokens[1]);
                    if (tSave != null) {
                        tSave.guardarADisco();
                    } else {
                        System.out.println("ERROR: La tabla '" + tokens[1] + "' no existe.");
                    }
                    break;

                case "HELP":
                    mostrarAyuda();
                    break;

                case "EXIT":
                    ejecutarExit();
                    break;

                default:
                    System.out.println("ERROR: Comando no reconocido '" + comandoPrincipal + "'. Escriba 'HELP' para asistencia.");
            }
        } catch (Exception e) {
            System.out.println("ERROR OPERACIONAL: " + e.getMessage());
        }
    }

    /**
     * Inserta un registro en memoria y ejecuta la persistencia síncrona inmediata .
     */
    private void ejecutarInsert(String[] tokens, String input) {
        if (tokens.length < 5 || !tokens[1].toUpperCase().equals("INTO") || !input.toUpperCase().contains("VALUES")) {
            System.out.println("ERROR: Sintaxis inválida. Uso correcto: INSERT INTO <tabla> VALUES (<id>, <nombre>, <nota>)");
            return;
        }

        String nombreTabla = tokens[2];
        Tabla tabla = dbManager.getTabla(nombreTabla);
        if (tabla == null) {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
            return;
        }

        try {
            int inicioParametros = input.indexOf("(");
            int finParametros = input.indexOf(")");
            if (inicioParametros == -1 || finParametros == -1 || finParametros < inicioParametros) {
                System.out.println("ERROR: Los parámetros de VALUES deben estar encerrados entre paréntesis.");
                return;
            }

            String rawParams = input.substring(inicioParametros + 1, finParametros);
            String[] params = rawParams.split(",");

            if (params.length != 3) {
                System.out.println("ERROR: Número de parámetros incorrecto. Se esperan 3 (ID, Nombre, Nota).");
                return;
            }

            int id = Integer.parseInt(params[0].trim());
            String nombre = params[1].trim();
            double nota = Double.parseDouble(params[2].trim());

            Row registro = new Row();
            registro.put("nombre", nombre);
            registro.put("nota", nota);

            // 1. Inserción e indexación en el árbol AVL (RAM)
            tabla.insertar(id, registro);
            
            // 2. Persistencia automática síncrona (Disco - Día 4)
            tabla.guardarADisco(); 
            System.out.println("Registro insertado correctamente. Archivo '" + nombreTabla + ".csv' sincronizado.");
            
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Conversión de tipos fallida. ID debe ser entero y Nota debe ser numérico.");
        }
    }

    /**
     * Realiza la búsqueda en el árbol AVL en O(log n).
     */
    private void ejecutarSelect(String[] tokens) {
        if (tokens.length < 7 || !tokens[1].toUpperCase().equals("FROM") || 
            !tokens[3].toUpperCase().equals("WHERE") || !tokens[4].toUpperCase().equals("ID") || 
            !tokens[5].equals("=")) {
            System.out.println("ERROR: Sintaxis inválida. Uso correcto: SELECT FROM <tabla> WHERE ID = <id>");
            return;
        }

        String nombreTabla = tokens[2];
        Tabla tabla = dbManager.getTabla(nombreTabla);
        if (tabla == null) {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
            return;
        }

        try {
            int idBuscar = Integer.parseInt(tokens[6]);
            Row resultado = tabla.buscar(idBuscar);

            if (resultado != null) {
                System.out.println("Registro encontrado (Indexación AVL): " + resultado);
            } else {
                System.out.println("Resultado: No se encontró ningún registro con el ID " + idBuscar);
            }
        } catch (NumberFormatException e) {
            System.out.println("ERROR: El valor del ID de búsqueda debe ser un número entero.");
        }
    }

    /**
     * Elimina la estructura de la memoria y destruye el archivo físico asociado.
     */
    private void ejecutarDrop(String nombreTabla) {
        if (dbManager.getTabla(nombreTabla) != null) {
            // 1. Remoción del catálogo en memoria RAM
            dbManager.borrarTabla(nombreTabla);
            
            // 2. Destrucción del archivo físico en disco 
            File archivoFisico = new File(nombreTabla + ".csv");
            if (archivoFisico.exists()) {
                if (archivoFisico.delete()) {
                    System.out.println("Archivo físico '" + nombreTabla + ".csv' eliminado del almacenamiento.");
                }
            }
            System.out.println("Tabla '" + nombreTabla + "' removida del sistema.");
        } else {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
        }
    }

    /**
     * guardado de todas las tablas en memoria antes de apagar el motor.
     */
    private void ejecutarExit() {
        System.out.println("Iniciando protocolo de cierre seguro...");
        
        // Barrido general sobre el catálogo de tablas para asegurar durabilidad
        for (String nombreTabla : dbManager.getListaTablas()) {
            Tabla tablaEnMemoria = dbManager.getTabla(nombreTabla);
            if (tablaEnMemoria != null) {
                tablaEnMemoria.guardarADisco();
            }
        }
        
        System.out.println("Todos los buffers de persistencia liberados. Motor apagado.");
        this.running = false;
    }

    private void mostrarAyuda() {
        System.out.println("\nComandos Soportados (Estructura Oficial SQL Simplificada):");
        System.out.println("  CREATE TABLE <nombre>                       : Inicializa una tabla en memoria / carga CSV.");
        System.out.println("  DROP TABLE <nombre>                         : Elimina la tabla de la memoria y del disco.");
        System.out.println("  INSERT INTO <nombre> VALUES (<id>,<n>,<nt>) : Inserta un registro con persistencia inmediata.");
        System.out.println("  SELECT FROM <nombre> WHERE ID = <id>        : Busca un registro indexado en O(log n).");
        System.out.println("  PRINT <nombre>                              : Muestra el contenido mediante recorrido Inorder.");
        System.out.println("  SAVE <nombre>                               : Fuerzo el volcado manual de los datos a disco.");
        System.out.println("  EXIT                                        : Cierre seguro garantizando el resguardo de datos.\n");
    }
}