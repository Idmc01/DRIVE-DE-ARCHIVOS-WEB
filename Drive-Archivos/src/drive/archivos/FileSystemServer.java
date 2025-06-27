package drive.archivos;
import static drive.archivos.CommandType.CREATE_DRIVE;
import static drive.archivos.CommandType.CREATE_FILE;
import static drive.archivos.CommandType.LOGIN;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
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
    private DriveUsuario usuarioActual;
    private static final String USERS_DIR = "users_data/";

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

    private String obtenerExtension(String nombre) {
    int punto = nombre.lastIndexOf('.');
    return (punto != -1) ? nombre.substring(punto) : "";
}
    private Nodo buscarNodoPorRuta(Nodo raiz, String ruta) {
    if (ruta.equals("/") || ruta.isEmpty()) return raiz;

    String[] partes = ruta.split("/");
    Nodo actual = raiz;

    for (String parte : partes) {
        if (parte.isEmpty()) continue;
        boolean encontrado = false;
        for (Nodo hijo : actual.contenidoLista) {
            if (hijo.nombre.equals(parte) && "directorio".equals(hijo.tipo)) {
                actual = hijo;
                encontrado = true;
                break;
            }
        }
        if (!encontrado) return null;
    }
    return actual;
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
            case CREATE_DIR:
                return handleCreateDir(command);
            case LIST_DIR:
                return handleListDir(command);
            case CHANGE_DIR:
                return handleChangeDir(command);

            // ... otros comandos
            default:
                return new CommandResponse(false, "Comando no reconocido");
        }
    }

    private CommandResponse handleCreateFile(Command command) {
        if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombreArchivo = (String) command.getParameters()[0];
    String contenido = (String) command.getParameters()[1];

    int tamano = contenido.getBytes().length;

    if ((usuarioActual.espacio_usado + tamano) > usuarioActual.espacio_total) {
        return new CommandResponse(false, "No hay suficiente espacio disponible.");
    }

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    // Validar duplicado
    for (Nodo hijo : actual.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo)) {
            return new CommandResponse(false, "Ya existe un archivo o directorio con ese nombre.");
        }
    }

    Nodo nuevo = new Nodo();
    nuevo.nombre = nombreArchivo;
    nuevo.tipo = "archivo";
    nuevo.extension = obtenerExtension(nombreArchivo);
    nuevo.contenido = contenido;
    nuevo.tamano = tamano;
    nuevo.fecha_creacion = nuevo.fecha_modificacion = java.time.LocalDateTime.now().toString();

    actual.contenidoLista.add(nuevo);
    usuarioActual.espacio_usado += tamano;

    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Archivo creado correctamente.");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar el archivo: " + e.getMessage());
    }
}

    private CommandResponse handleLogin(Command command) {
         String username = (String) command.getParameters()[0];
    try {
        DriveUsuario cargado = GestorJson.cargarDrive(username);
        if (cargado == null) {
            return new CommandResponse(false, "Usuario no encontrado");
        }

        this.currentUser = username;
        this.usuarioActual = cargado;
        return new CommandResponse(true, "Login exitoso");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al cargar usuario");
    }
}

    private CommandResponse handleCreateDrive(Command command) {
        String username = (String) command.getParameters()[0];
        long espacioMax = (Long) command.getParameters()[1];
        
        try{
            File jsonFile = new File(USERS_DIR + username + ".json");
            if (jsonFile.exists()){
                return new CommandResponse(false, "El usuario ya existe");
            }
            DriveUsuario nuevo = new DriveUsuario();
            nuevo.usuario = username;
            nuevo.espacio_total = (int) espacioMax;
            nuevo.espacio_usado = 0;
            nuevo.ruta_actual = "/";

            Nodo root = new Nodo();
            root.nombre = "root";
            root.tipo = "directorio";
            root.fecha_creacion = root.fecha_modificacion = java.time.LocalDateTime.now().toString();

            Nodo compartidos = new Nodo();
            compartidos.nombre = "compartidos";
            compartidos.tipo = "directorio";
            compartidos.fecha_creacion = compartidos.fecha_modificacion = java.time.LocalDateTime.now().toString();

            nuevo.drive = root;
            nuevo.compartidos = compartidos;

            GestorJson.guardarDrive(nuevo);
            return new CommandResponse(true, "Drive creado exitosamente");
        } catch (IOException e){
            return new CommandResponse(false, "Error al crear drive: " + e.getMessage());
        }
    }
    private CommandResponse handleCreateDir(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombreDir = (String) command.getParameters()[0];

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    for (Nodo hijo : actual.contenidoLista) {
        if (hijo.nombre.equals(nombreDir)) {
            return new CommandResponse(false, "Ya existe un archivo o directorio con ese nombre.");
        }
    }

    Nodo nuevoDir = new Nodo();
    nuevoDir.nombre = nombreDir;
    nuevoDir.tipo = "directorio";
    nuevoDir.fecha_creacion = nuevoDir.fecha_modificacion = java.time.LocalDateTime.now().toString();
    nuevoDir.contenidoLista = new ArrayList<>();

    actual.contenidoLista.add(nuevoDir);

    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Directorio creado correctamente.");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar: " + e.getMessage());
    }
}
    private CommandResponse handleListDir(Command command) {
        if (usuarioActual == null) {
            return new CommandResponse(false, "Debe hacer login primero.");
    }

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    if (actual.contenidoLista.isEmpty()) {
        return new CommandResponse(true, "(Vacío)");
    }

    StringBuilder salida = new StringBuilder();
    for (Nodo hijo : actual.contenidoLista) {
        if ("directorio".equals(hijo.tipo)) {
            salida.append("[DIR]  ").append(hijo.nombre).append("\n");
        } else {
            salida.append("[FILE] ").append(hijo.nombre)
                  .append(" (").append(hijo.tamano).append(" bytes)").append("\n");
        }
    }

    return new CommandResponse(true, "Contenido listado.", salida.toString());
}
    private CommandResponse handleChangeDir(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String destino = (String) command.getParameters()[0];

    String nuevaRuta;
    if (destino.equals("/")) {
        nuevaRuta = "/";
    } else if (destino.equals("..")) {
        if (usuarioActual.ruta_actual.equals("/")) {
            return new CommandResponse(false, "Ya está en la raíz.");
        }
        int lastSlash = usuarioActual.ruta_actual.lastIndexOf('/');
        nuevaRuta = usuarioActual.ruta_actual.substring(0, lastSlash);
        if (nuevaRuta.isEmpty()) nuevaRuta = "/";
    } else {
        if (!usuarioActual.ruta_actual.endsWith("/")) {
            usuarioActual.ruta_actual += "/";
        }
        nuevaRuta = usuarioActual.ruta_actual + destino;
    }

    Nodo destinoNodo = buscarNodoPorRuta(usuarioActual.drive, nuevaRuta);
    if (destinoNodo == null || !"directorio".equals(destinoNodo.tipo)) {
        return new CommandResponse(false, "Directorio no encontrado.");
    }

    usuarioActual.ruta_actual = nuevaRuta;
    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Directorio cambiado a " + nuevaRuta, nuevaRuta);
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar ruta: " + e.getMessage());
    }
}

    // Métodos para manejar cada comando específico...
}