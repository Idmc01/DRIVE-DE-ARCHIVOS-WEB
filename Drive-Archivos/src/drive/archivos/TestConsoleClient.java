package drive.archivos;
import java.util.Scanner;

public class TestConsoleClient {
    private static FileSystemClient client;
    private static String currentUser = null;
    private static String currentPath = "/";

    public static void main(String[] args) {
        client = new FileSystemClient();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Cliente de Prueba del Drive Web ===");

        // Conexión inicial
        if (!connectToServer(scanner)) {
            return;
        }

        // Menú principal
        boolean running = true;
        while (running) {
            System.out.println("\n=== MENÚ ===");
            System.out.println("1. Crear Drive");
            System.out.println("2. Login");
            System.out.println("3. Operaciones con archivos");
            System.out.println("4. Salir");
            System.out.print("Seleccione una opción: ");

            int option = scanner.nextInt();
            scanner.nextLine(); // Consumir el salto de línea

            switch (option) {
                case 1:
                    testCreateDrive(scanner);
                    break;
                case 2:
                    testLogin(scanner);
                    break;
                case 3:
                    if (currentUser != null) {
                        testFileOperations(scanner);
                    } else {
                        System.out.println("Debe hacer login primero!");
                    }
                    break;
                case 4:
                    running = false;
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        }

        client.disconnect();
        scanner.close();
    }

    private static boolean connectToServer(Scanner scanner) {
        System.out.print("Dirección del servidor (localhost): ");
        String host = scanner.nextLine();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Puerto (12345): ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 12345 : Integer.parseInt(portStr);

        System.out.println("Conectando al servidor...");
        return client.connect(host, port);
    }

    private static void testCreateDrive(Scanner scanner) {
        System.out.print("Nombre de usuario: ");
        String username = scanner.nextLine();

        System.out.print("Espacio en bytes (1048576 = 1MB): ");
        long space = scanner.nextLong();
        scanner.nextLine(); // Consumir el salto de línea

        boolean success = client.createDrive(username, space);
        System.out.println(success ? "Drive creado exitosamente" : "Error al crear drive");
    }

    private static void testLogin(Scanner scanner) {
        System.out.print("Nombre de usuario: ");
        String username = scanner.nextLine();

        boolean success = client.login(username);
        if (success) {
            currentUser = username;
            System.out.println("Login exitoso");
        } else {
            System.out.println("Error en login");
        }
    }

    private static void testFileOperations(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\nRuta actual: " + currentPath);
            System.out.println("1. Crear archivo");
            System.out.println("2. Crear directorio");
            System.out.println("3. Listar contenido");
            System.out.println("4. Cambiar directorio");
            System.out.println("5. Volver al menú principal");
            System.out.print("Seleccione operación: ");

            int op = scanner.nextInt();
            scanner.nextLine(); // Consumir el salto de línea

            switch (op) {
                case 1:
                    System.out.print("Nombre del archivo: ");
                    String filename = scanner.nextLine();
                    System.out.print("Contenido: ");
                    String content = scanner.nextLine();
                    CommandResponse res = client.createFile(filename, content);
                    System.out.println(res.isSuccess() ? "Archivo creado" : "Error: " + res.getMessage());
                    break;
                case 2:
                    System.out.print("Nombre del directorio: ");
                    String dirname = scanner.nextLine();
                    res = client.createDirectory(dirname);
                    System.out.println(res.isSuccess() ? "Directorio creado" : "Error: " + res.getMessage());
                    break;
                case 3:
                    res = client.listDirectory();
                    if (res.isSuccess()) {
                        System.out.println("Contenido:");
                        System.out.println(res.getData()); // Asume que data contiene la lista
                    } else {
                        System.out.println("Error: " + res.getMessage());
                    }
                    break;
                case 4:
                    System.out.print("Nuevo directorio (.. para subir): ");
                    String newDir = scanner.nextLine();
                    res = client.changeDirectory(newDir);
                    if (res.isSuccess()) {
                        currentPath = (String) res.getData(); // Asume que data contiene la nueva ruta
                    } else {
                        System.out.println("Error: " + res.getMessage());
                    }
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }
}