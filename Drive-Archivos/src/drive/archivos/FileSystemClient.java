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
    public CommandResponse viewFile(String filename) {
    Command cmd = new Command(CommandType.VIEW_FILE, filename);
    return sendCommand(cmd);
}
    public CommandResponse modifyFile(String filename, String newContent) {
    Command cmd = new Command(CommandType.MODIFY_FILE, filename, newContent);
    return sendCommand(cmd);
}
    public CommandResponse viewProperties(String filename) {
    Command cmd = new Command(CommandType.VIEW_PROPERTIES, filename);
    return sendCommand(cmd);
}
    public CommandResponse delete(String name) {
    Command cmd = new Command(CommandType.DELETE, name);
    return sendCommand(cmd);
}
    public CommandResponse copy(String name, String sourcePath, String destPath) {
    Command cmd = new Command(CommandType.COPY, name, sourcePath, destPath);
    return sendCommand(cmd);
}
    public CommandResponse move(String name, String sourcePath, String destPath) {
        Command cmd = new Command(CommandType.MOVE, name, sourcePath, destPath);
        return sendCommand(cmd);
    }
    public CommandResponse share(String name, String sourcePath, String targetUsername) {
    Command cmd = new Command(CommandType.SHARE, name, sourcePath, targetUsername);
    return sendCommand(cmd);
}
    public CommandResponse downloadFile(String filename, String path) {
    Command cmd = new Command(CommandType.DOWNLOAD, filename, path);
    return sendCommand(cmd);
}
public CommandResponse loadFile(String localPath, String currentPath) {
    Command cmd = new Command(CommandType.LOAD, localPath, currentPath);
    return sendCommand(cmd);
}



}
