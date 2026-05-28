public class Main {
    public static void main(String[] args) {
        // Inicialización del gestor de almacenamiento 
        DatabaseManager dbManager = new DatabaseManager();

        // Inyección de dependencias en el componente de interfaz de usuario
        ConsoleInterface cli = new ConsoleInterface(dbManager);

        // Arranque del bucle de ejecución de la terminal
        cli.start();
    }
}
