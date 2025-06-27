package drive.archivos;
import java.io.*;
import java.net.*;

public class FileSystemClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String currentPath = "/";

    public boolean connect(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar al servidor: " + e.getMessage());
            return false;
        }
    }

    public CommandResponse sendCommand(Command command) {
        try {
            out.writeObject(command);
            out.flush();
            return (CommandResponse) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al enviar comando: " + e.getMessage());
            return new CommandResponse(false, "Error de comunicación");
        }
    }

    // Métodos específicos para cada operación
    public boolean createDrive(String username, long maxBytes) {
        Command cmd = new Command(CommandType.CREATE_DRIVE, username, maxBytes);
        CommandResponse response = sendCommand(cmd);
        return response.isSuccess();
    }

    public boolean login(String username) {
        Command cmd = new Command(CommandType.LOGIN, username);
        CommandResponse response = sendCommand(cmd);
        if (response.isSuccess()) {
            this.currentPath = "/";
        }
        return response.isSuccess();
    }

    // ... otros métodos para las operaciones del filesystem

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error al desconectar: " + e.getMessage());
        }
    }

    public CommandResponse createFile(String filename, String content) {
        Command cmd = new Command(CommandType.CREATE_FILE, filename, content);
    return sendCommand(cmd);
}

    public CommandResponse createDirectory(String dirname) {
        Command cmd = new Command(CommandType.CREATE_DIR, dirname);
    return sendCommand(cmd);
    }

    public CommandResponse listDirectory() {
        Command cmd = new Command(CommandType.LIST_DIR);
    return sendCommand(cmd);
    }

    public CommandResponse changeDirectory(String newDir) {
        Command cmd = new Command(CommandType.CHANGE_DIR, newDir);
    return sendCommand(cmd);
    }
}
