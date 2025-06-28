package drive.archivos;
import java.io.FileWriter;
import java.io.IOException;
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
            System.out.println("1.  Crear archivo");
            System.out.println("2.  Crear directorio");
            System.out.println("3.  Listar contenido");
            System.out.println("4.  Cambiar directorio");
            System.out.println("5.  Ver contenido de un archivo");
            System.out.println("6.  Modificar archivo");
            System.out.println("7.  Ver propoiedades del archivo");
            System.out.println("8.  Eliminar");
            System.out.println("9.  Copiar");
            System.out.println("10. Mover");
            System.out.println("11. Compartir");
            System.out.println("12. descargar");
            //el resto
            System.out.println("13. Volver al menú principal");
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
                    System.out.print("Nombre del archivo a ver: ");
                    String viewName = scanner.nextLine();
                    res = client.viewFile(viewName);
                    if (res.isSuccess()) {
                        System.out.println("Contenido:");
                        System.out.println(res.getData());
                    } else {
                        System.out.println("Error: " + res.getMessage());
                    }
                    break;
                case 6:
                    System.out.print("Nombre del archivo a modificar: ");
                    String modName = scanner.nextLine();
                    System.out.print("Nuevo contenido: ");
                    String newContent = scanner.nextLine();
                    res = client.modifyFile(modName, newContent);
                    System.out.println(res.isSuccess() ? "Archivo modificado" : "Error: " + res.getMessage());
                    break;
                case 7:
                    System.out.print("Nombre del archivo: ");
                    String propName = scanner.nextLine();
                    res = client.viewProperties(propName);
                    if (res.isSuccess()) {
                        System.out.println("Propiedades:");
                        System.out.println(res.getData());
                    } else {
                        System.out.println("Error: " + res.getMessage());
                    }
                    break;
                case 8:
                    System.out.print("Nombre del archivo o directorio a eliminar: ");
                    String deleteName = scanner.nextLine();
                    res = client.delete(deleteName);
                    System.out.println(res.isSuccess() ? "Eliminado con éxito" : "Error: " + res.getMessage());
                    break;
                case 9:
                    System.out.print("Nombre del archivo/directorio a copiar: ");
    String nameToCopy = scanner.nextLine();
    String originPath = currentPath;

    // Navegación para seleccionar destino
    String targetPath = currentPath;
    boolean choosing = true;
    while (choosing) {
        System.out.println("Destino actual: " + targetPath);
        System.out.println("1. Cambiar directorio");
        System.out.println("2. Copiar aquí");
        System.out.println("3. Cancelar");
        System.out.print("Seleccione opción: ");
        int subOp = scanner.nextInt(); scanner.nextLine();

        switch (subOp) {
            case 1:
                System.out.print("Nuevo directorio (.. para subir): ");
                String temp = scanner.nextLine();
                CommandResponse resp = client.changeDirectory(temp);
                if (resp.isSuccess()) {
                    targetPath = (String) resp.getData();
                    currentPath = targetPath;
                } else {
                    System.out.println("Error: " + resp.getMessage());
                }
                break;
            case 2:
                CommandResponse copyResp = client.copy(nameToCopy, originPath, targetPath);
                System.out.println(copyResp.isSuccess() ? "Copiado correctamente" : "Error: " + copyResp.getMessage());
                choosing = false;
                break;
            case 3:
                choosing = false;
                System.out.println("Copia cancelada.");
                break;
            default:
                System.out.println("Opción no válida");
        }
    }
    break;
                case 10:
                System.out.print("Nombre del archivo/directorio a mover: ");
    String nameToMove = scanner.nextLine();
    String moveOrigin = currentPath;
    String moveTarget = currentPath;
    boolean moving = true;
    while (moving) {
        System.out.println("Destino actual: " + moveTarget);
        System.out.println("1. Cambiar directorio");
        System.out.println("2. Mover aquí");
        System.out.println("3. Cancelar");
        System.out.print("Seleccione opción: ");
        int sub = scanner.nextInt(); scanner.nextLine();
        switch (sub) {
            case 1:
                System.out.print("Nuevo directorio (.. para subir): ");
                String temp = scanner.nextLine();
                CommandResponse moveDir = client.changeDirectory(temp);
                if (moveDir.isSuccess()) {
                    moveTarget = (String) moveDir.getData();
                    currentPath = moveTarget;
                } else {
                    System.out.println("Error: " + moveDir.getMessage());
                }
                break;
            case 2:
                CommandResponse moveResp = client.move(nameToMove, moveOrigin, moveTarget);
                System.out.println(moveResp.isSuccess() ? "Movido correctamente" : "Error: " + moveResp.getMessage());
                moving = false;
                break;
            case 3:
                System.out.println("Movimiento cancelado.");
                moving = false;
                break;
            default:
                System.out.println("Opción no válida.");
        }
    }
    break;   
                case 11:
                System.out.print("Nombre del archivo/directorio a compartir: ");
    String shareName = scanner.nextLine();

    System.out.print("Usuario con quien compartir: ");
    String targetUser = scanner.nextLine();

    CommandResponse shareResp = client.share(shareName, currentPath, targetUser);
    System.out.println(shareResp.isSuccess() ? "Compartido correctamente" : "Error: " + shareResp.getMessage());
    break;  
                case 12:
                    System.out.print("Nombre del archivo a descargar: ");
    String nombreArchivo = scanner.nextLine();

    CommandResponse downloadRes = client.downloadFile(nombreArchivo, currentPath);
    if (downloadRes.isSuccess()) {
        Nodo archivoDescargado = (Nodo) downloadRes.getData();
        try {
            String downloadPath = "users_data/" + currentUser + "/";
            FileWriter writer = new FileWriter(downloadPath);
            writer.write(archivoDescargado.contenido);
            writer.close();
            System.out.println("Archivo descargado en: " + downloadPath);
        } catch (IOException e) {
            System.out.println("Error al guardar el archivo: " + e.getMessage());
        }
    } else {
        System.out.println("Error: " + downloadRes.getMessage());
    }
    break;
                case 13:
                    System.out.print("Ruta del archivo local (ej. C:\\\\Users\\\\usuario\\\\archivo.txt): ");
    String localPath = scanner.nextLine();

    CommandResponse loadRes = client.loadFile(localPath, currentPath);
    System.out.println(loadRes.isSuccess() ? "Archivo subido correctamente." : "Error: " + loadRes.getMessage());
    break;
                //continuar aqui
                case 14:
                    back = true;
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }
}