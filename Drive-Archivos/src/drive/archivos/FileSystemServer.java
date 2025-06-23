package drive.archivos;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FileSystemServer {
    private static final int PORT = 12345;
    private static final int MAX_THREADS = 10;
    private static final String USERS_DIR = "users_data/";

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de File System iniciado en el puerto " + PORT);

            // Crear directorio para datos de usuarios si no existe
            new File(USERS_DIR).mkdirs();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String currentUser;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            // Protocolo de comunicación
            while (true) {
                Command command = (Command) in.readObject();
                CommandResponse response = processCommand(command);
                out.writeObject(response);
                out.flush();
            }

        } catch (EOFException e) {
            System.out.println("Cliente desconectado: " + clientSocket);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error con cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private CommandResponse processCommand(Command command) {
        // Implementar lógica para cada comando
        switch (command.getType()) {
            case CREATE_DRIVE:
                return handleCreateDrive(command);
            case LOGIN:
                return handleLogin(command);
            case CREATE_FILE:
                return handleCreateFile(command);
            // ... otros comandos
            default:
                return new CommandResponse(false, "Comando no reconocido");
        }
    }

    private CommandResponse handleCreateFile(Command command) {
        return handleCreateFile(command);
    }

    private CommandResponse handleLogin(Command command) {
        return handleLogin(command);
    }

    private CommandResponse handleCreateDrive(Command command) {
        return handleCreateDrive(command);
    }

    // Métodos para manejar cada comando específico...
}