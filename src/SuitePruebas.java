import java.io.File;

public class SuitePruebas {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO SUITE DE PRUEBAS AUTOMATIZADA DEL MOTOR AVL ===");
        DatabaseManager dbManager = new DatabaseManager();
        
        try {
            // Limpieza previa de entorno
            File archivoViejo = new File("tabla_test.csv");
            if (archivoViejo.exists()) archivoViejo.delete();

            // 1. TEST: Creación de Espacio de Almacenamiento
            dbManager.crearTabla("tabla_test");
            Tabla tabla = dbManager.getTabla("tabla_test");
            assertTest(tabla != null, "Creación de tabla e inicialización en RAM");

            // 2. TEST: Inserciones y Autobalanceo AVL (Provocando Rotación)
            Row r1 = new Row(); r1.put("nombre", "UserA"); r1.put("nota", 4.0);
            Row r2 = new Row(); r2.put("nombre", "UserB"); r2.put("nota", 3.5);
            Row r3 = new Row(); r3.put("nombre", "UserC"); r3.put("nota", 4.8);

            // Inserción secuencial (Fuerza rotación en raíz original si no balanceara)
            tabla.insertar(10, r1);
            tabla.insertar(20, r2);
            tabla.insertar(30, r3);
            
            assertTest(tabla.getCantidadRegistros() == 3, "Inserción exitosa de 3 registros");
            assertTest(tabla.getArbol().getRoot().key == 20, "Verificación de Autobalanceo AVL (Raíz rotó correctamente a 20)");

            // 3. TEST: Búsqueda Logarítmica Exacta
            Row buscado = tabla.buscar(30);
            assertTest(buscado != null && buscado.get("nombre").equals("UserC"), "Búsqueda exacta en O(log n)");

            // 4. TEST: Actualización Atómica (UPDATE)
            tabla.actualizarRegistro(20, "nota", 5.0);
            assertTest(Double.compare((Double)tabla.buscar(20).get("nota"), 5.0) == 0, "Actualización mutable de campos");

            // 5. TEST: Eliminación con Rebalanceo (DELETE)
            tabla.eliminarRegistro(10);
            assertTest(tabla.getCantidadRegistros() == 2, "Eliminación física del registro ID 10");
            assertTest(tabla.buscar(10) == null, "Confirmación de ausencia del nodo eliminado");

            // 6. TEST: Persistencia y Recuperación tras Reinicio
            tabla.guardarADisco();
            assertTest(new File("tabla_test.csv").exists(), "Verificación de existencia del archivo plano .csv");

            // Simulación de reinicio del motor (Nueva instancia leyendo disco)
            DatabaseManager dbManagerReinicio = new DatabaseManager();
            dbManagerReinicio.crearTabla("tabla_test");
            Tabla tablaRestaurada = dbManagerReinicio.getTabla("tabla_test");

            assertTest(tablaRestaurada.getCantidadRegistros() == 2, "Recuperación e integridad de datos tras reinicio completo del sistema");
            assertTest(tablaRestaurada.buscar(30) != null, "Consistencia del índice AVL restaurado");

            System.out.println("\n>>> ¡RESULTADO FINAL: TODAS LAS PRUEBAS UNITARIAS PASARON EXITOSAMENTE (100%) <<<");

        } catch (Exception e) {
            System.out.println("\n[FALLO EN LA SUITE]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void assertTest(boolean condicion, String descripcion) {
        if (condicion) {
            System.out.println("  [PASÓ]: " + descripcion);
        } else {
            throw new RuntimeException("Fallo crítico en el caso de prueba: " + descripcion);
        }
    }
}