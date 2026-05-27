import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ConsoleInterface {
    private DatabaseManager dbManager;
    private boolean running;

    public ConsoleInterface(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.running = true;
    }

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

    private void procesarComando(String input) {
        String[] tokens = input.split("\\s+");
        String comandoPrincipal = tokens[0].toUpperCase();

        try {
            switch (comandoPrincipal) {
                case "CREATE":
                    if (tokens.length < 3 || !tokens[1].toUpperCase().equals("TABLE")) {
                        System.out.println("ERROR: Sintaxis inválida. Uso: CREATE TABLE <nombre_tabla>");
                        return;
                    }
                    dbManager.crearTabla(tokens[2]);
                    break;

                case "DROP":
                    if (tokens.length < 3 || !tokens[1].toUpperCase().equals("TABLE")) {
                        System.out.println("ERROR: Sintaxis inválida. Uso: DROP TABLE <nombre_tabla>");
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

                case "UPDATE":
                    ejecutarUpdate(tokens);
                    break;

                case "DELETE":
                    ejecutarDelete(tokens);
                    break;

                case "PRINT":
                    if (tokens.length < 2) {
                        System.out.println("ERROR: Sintaxis inválida. Uso: PRINT <nombre_tabla>");
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
                        System.out.println("ERROR: Sintaxis inválida. Uso: SAVE <nombre_tabla>");
                        return;
                    }
                    Tabla tSave = dbManager.getTabla(tokens[1]);
                    if (tSave != null) {
                        tSave.guardarADisco();
                        System.out.println("Volcado manual a disco completado de forma exitosa.");
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
                    System.out.println("ERROR: Comando no reconocido '" + comandoPrincipal + "'.");
            }
        } catch (Exception e) {
            System.out.println("ERROR OPERACIONAL CRÍTICO: " + e.getMessage());
        }
    }

    private void ejecutarInsert(String[] tokens, String input) {
        if (tokens.length < 5 || !tokens[1].toUpperCase().equals("INTO") || !input.toUpperCase().contains("VALUES")) {
            System.out.println("ERROR: Sintaxis inválida. Uso: INSERT INTO <tabla> VALUES (<id>, <nombre>, <nota>)");
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
                System.out.println("ERROR: Número de parámetros incorrecto.");
                return;
            }

            int id = Integer.parseInt(params[0].trim());
            String nombre = params[1].trim();
            double nota = Double.parseDouble(params[2].trim());

            Row registro = new Row();
            registro.put("nombre", nombre);
            registro.put("nota", nota);

            tabla.insertar(id, registro);
            System.out.println("Registro insertado correctamente. Archivo '" + nombreTabla + ".csv' sincronizado.");
            
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Conversión de tipos fallida.");
        } catch (IOException e) {
            System.out.println("ERROR TRANSACCIONAL: Fallo en disco. Transacción abortada (Atomicidad Rollback aplicada).");
        }
    }

    private void ejecutarSelect(String[] tokens) {
        if (tokens.length < 7 || !tokens[1].toUpperCase().equals("FROM") || 
            !tokens[3].toUpperCase().equals("WHERE") || !tokens[4].toUpperCase().equals("ID")) {
            System.out.println("ERROR: Sintaxis inválida. Uso: SELECT FROM <tabla> WHERE ID <operador> <valor>");
            return;
        }

        String nombreTabla = tokens[2];
        Tabla tabla = dbManager.getTabla(nombreTabla);
        if (tabla == null) {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
            return;
        }

        String operador = tokens[5];
        try {
            int idRef = Integer.parseInt(tokens[6]);

            if (operador.equals("=")) {
                Row resultado = tabla.buscar(idRef);
                if (resultado != null) {
                    System.out.println("Registro encontrado: " + resultado);
                } else {
                    System.out.println("Resultado: No se encontraron registros.");
                }
            } else if (operador.equals(">") || operador.equals("<") || operador.equals(">=") || operador.equals("<=")) {
                List<String> registrosFiltrados = tabla.buscarPorRango(operador, idRef);
                if (!registrosFiltrados.isEmpty()) {
                    for (String reg : registrosFiltrados) {
                        System.out.println(reg);
                    }
                } else {
                    System.out.println("Resultado: No se encontraron registros.");
                }
            } else {
                System.out.println("ERROR: Operador relacional no soportado '" + operador + "'.");
            }
        } catch (NumberFormatException e) {
            System.out.println("ERROR: El valor de comparación debe ser un número entero.");
        }
    }

    private void ejecutarUpdate(String[] tokens) {
        if (tokens.length < 10 || !tokens[2].toUpperCase().equals("SET") || 
            !tokens[4].equals("=") || !tokens[6].toUpperCase().equals("WHERE") || 
            !tokens[7].toUpperCase().equals("ID") || !tokens[8].equals("=")) {
            System.out.println("ERROR: Sintaxis inválida. Uso: UPDATE <tabla> SET <campo> = <valor> WHERE ID = <id>");
            return;
        }

        String nombreTabla = tokens[1];
        Tabla tabla = dbManager.getTabla(nombreTabla);
        if (tabla == null) {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
            return;
        }

        String campo = tokens[3].toLowerCase();
        String nuevoValorRaw = tokens[5];

        try {
            int idTarget = Integer.parseInt(tokens[9]);
            Object valorTipado = nuevoValorRaw;
            
            if (campo.equals("nota")) {
                valorTipado = Double.parseDouble(nuevoValorRaw);
            }

            tabla.actualizarRegistro(idTarget, campo, valorTipado);
            System.out.println("Registro " + idTarget + " modificado e indexado con éxito.");
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Error de conversión de tipo.");
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR TRANSACCIONAL: Fallo de persistencia. Cambios revertidos.");
        }
    }

    private void ejecutarDelete(String[] tokens) {
        if (tokens.length < 7 || !tokens[1].toUpperCase().equals("FROM") || 
            !tokens[3].toUpperCase().equals("WHERE") || !tokens[4].toUpperCase().equals("ID") || 
            !tokens[5].equals("=")) {
            System.out.println("ERROR: Sintaxis inválida. Uso: DELETE FROM <tabla> WHERE ID = <id>");
            return;
        }

        String nombreTabla = tokens[2];
        Tabla tabla = dbManager.getTabla(nombreTabla);
        if (tabla == null) {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
            return;
        }

        try {
            int idTarget = Integer.parseInt(tokens[6]);
            tabla.eliminarRegistro(idTarget);
            System.out.println("Registro " + idTarget + " purgado del árbol AVL y del archivo físico con éxito.");
        } catch (NumberFormatException e) {
            System.out.println("ERROR: El argumento ID debe ser un número entero.");
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR TRANSACCIONAL: Abortando borrado. Estructura RAM restaurada.");
        }
    }

    private void ejecutarDrop(String nombreTabla) {
        if (dbManager.getTabla(nombreTabla) != null) {
            dbManager.borrarTabla(nombreTabla);
            File archivoFisico = new File(nombreTabla + ".csv");
            if (archivoFisico.exists()) {
                archivoFisico.delete();
            }
            System.out.println("Tabla '" + nombreTabla + "' removida del sistema.");
        } else {
            System.out.println("ERROR: La tabla '" + nombreTabla + "' no existe.");
        }
    }

    private void ejecutarExit() {
        System.out.println("Iniciando protocolo de cierre seguro...");
        for (String nombreTabla : dbManager.getListaTablas()) {
            Tabla tablaEnMemoria = dbManager.getTabla(nombreTabla);
            if (tablaEnMemoria != null) {
                try {
                    tablaEnMemoria.guardarADisco();
                } catch (IOException e) {
                    System.out.println("Fallo al salvar " + nombreTabla);
                }
            }
        }
        System.out.println("Todos los buffers de persistencia liberados. Motor apagado.");
        this.running = false;
    }

    private void mostrarAyuda() {
        System.out.println("\nEstructura de Comandos Soportados:");
        System.out.println("  CREATE TABLE <nombre>");
        System.out.println("  DROP TABLE <nombre>");
        System.out.println("  INSERT INTO <nombre> VALUES (<id>, <nombre>, <nota>)");
        System.out.println("  SELECT FROM <nombre> WHERE ID <op> <valor>");
        System.out.println("  UPDATE <nombre> SET <campo> = <valor> WHERE ID = <id>");
        System.out.println("  DELETE FROM <nombre> WHERE ID = <id>");
        System.out.println("  PRINT <nombre>");
        System.out.println("  EXIT\n");
    }
}