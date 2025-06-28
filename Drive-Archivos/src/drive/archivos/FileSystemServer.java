package drive.archivos;
import static drive.archivos.CommandType.CREATE_DRIVE;
import static drive.archivos.CommandType.CREATE_FILE;
import static drive.archivos.CommandType.LOGIN;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
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
    private String nombreUsuarioActual;


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
            case VIEW_FILE:
                return handleViewFile(command);
            case MODIFY_FILE:
                return handleModifyFile(command);
            case VIEW_PROPERTIES:
                return handleViewProperties(command);
            case DELETE:
                return handleDelete(command);
            case COPY:
                return handleCopy(command);
            case MOVE:
                return handleMove(command);
            case SHARE:
                return handleShare(command);
            case DOWNLOAD:
                return handleDownload(command);
            case LOAD:
                return handleLoad(command);


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
        this.nombreUsuarioActual = username; // <- esta línea es nueva
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
    private CommandResponse handleViewFile(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombreArchivo = (String) command.getParameters()[0];

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    for (Nodo hijo : actual.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo) && "archivo".equals(hijo.tipo)) {
            return new CommandResponse(true, "Contenido del archivo:", hijo.contenido);
        }
    }

    return new CommandResponse(false, "Archivo no encontrado.");
}
    private CommandResponse handleModifyFile(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombreArchivo = (String) command.getParameters()[0];
    String nuevoContenido = (String) command.getParameters()[1];

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    for (Nodo hijo : actual.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo) && "archivo".equals(hijo.tipo)) {
            int nuevoTamano = nuevoContenido.getBytes().length;
            int diferencia = nuevoTamano - hijo.tamano;

            if ((usuarioActual.espacio_usado + diferencia) > usuarioActual.espacio_total) {
                return new CommandResponse(false, "No hay suficiente espacio disponible.");
            }

            hijo.contenido = nuevoContenido;
            hijo.tamano = nuevoTamano;
            hijo.fecha_modificacion = java.time.LocalDateTime.now().toString();
            usuarioActual.espacio_usado += diferencia;

            try {
                GestorJson.guardarDrive(usuarioActual);
                return new CommandResponse(true, "Archivo modificado correctamente.");
            } catch (IOException e) {
                return new CommandResponse(false, "Error al guardar: " + e.getMessage());
            }
        }
    }

    return new CommandResponse(false, "Archivo no encontrado.");
}
    private CommandResponse handleViewProperties(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombreArchivo = (String) command.getParameters()[0];

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    for (Nodo hijo : actual.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo) && "archivo".equals(hijo.tipo)) {
            StringBuilder props = new StringBuilder();
            props.append("Nombre: ").append(hijo.nombre).append("\n");
            props.append("Extensión: ").append(hijo.extension).append("\n");
            props.append("Tamaño: ").append(hijo.tamano).append(" bytes\n");
            props.append("Fecha de creación: ").append(hijo.fecha_creacion).append("\n");
            props.append("Última modificación: ").append(hijo.fecha_modificacion).append("\n");

            return new CommandResponse(true, "Propiedades del archivo:", props.toString());
        }
    }

    return new CommandResponse(false, "Archivo no encontrado.");
}
    private CommandResponse handleDelete(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login primero.");
    }

    String nombre = (String) command.getParameters()[0];

    Nodo actual = buscarNodoPorRuta(usuarioActual.drive, usuarioActual.ruta_actual);
    if (actual == null || !"directorio".equals(actual.tipo)) {
        return new CommandResponse(false, "Directorio actual inválido.");
    }

    Iterator<Nodo> iterator = actual.contenidoLista.iterator();
    while (iterator.hasNext()) {
        Nodo hijo = iterator.next();
        if (hijo.nombre.equals(nombre)) {
            int espacioLiberado = calcularEspacio(hijo);
            iterator.remove();
            usuarioActual.espacio_usado -= espacioLiberado;

            try {
                GestorJson.guardarDrive(usuarioActual);
                return new CommandResponse(true, "Elemento eliminado correctamente.");
            } catch (IOException e) {
                return new CommandResponse(false, "Error al guardar: " + e.getMessage());
            }
        }
    }

    return new CommandResponse(false, "Archivo o directorio no encontrado.");
}
    private int calcularEspacio(Nodo nodo) {
    if ("archivo".equals(nodo.tipo)) {
        return nodo.tamano;
    } else if ("directorio".equals(nodo.tipo)) {
        int total = 0;
        for (Nodo hijo : nodo.contenidoLista) {
            total += calcularEspacio(hijo);
        }
        return total;
    }
    return 0;
}
    private CommandResponse handleCopy(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login.");
    }

    String nombre = (String) command.getParameters()[0];
    String rutaOrigen = (String) command.getParameters()[1];
    String rutaDestino = (String) command.getParameters()[2];

    Nodo dirOrigen = buscarNodoPorRuta(usuarioActual.drive, rutaOrigen);
    Nodo dirDestino = buscarNodoPorRuta(usuarioActual.drive, rutaDestino);

    if (dirOrigen == null || !"directorio".equals(dirOrigen.tipo)) {
        return new CommandResponse(false, "Ruta de origen inválida.");
    }
    if (dirDestino == null || !"directorio".equals(dirDestino.tipo)) {
        return new CommandResponse(false, "Ruta de destino inválida.");
    }

    Nodo original = null;
    for (Nodo hijo : dirOrigen.contenidoLista) {
        if (hijo.nombre.trim().equalsIgnoreCase(nombre.trim())) {
            original = hijo;
            break;
        }
    }

    if (original == null) {
        return new CommandResponse(false, "Elemento a copiar no encontrado.");
    }

    for (Nodo hijo : dirDestino.contenidoLista) {
        if (hijo.nombre.equals(original.nombre)) {
            return new CommandResponse(false, "Ya existe un elemento con ese nombre en el destino.");
        }
    }

    Nodo copia = clonarNodo(original);
    copia.fecha_creacion = java.time.LocalDateTime.now().toString();
    copia.fecha_modificacion = copia.fecha_creacion;

    int espacioNecesario = calcularEspacio(copia);
    if (usuarioActual.espacio_usado + espacioNecesario > usuarioActual.espacio_total) {
        return new CommandResponse(false, "Espacio insuficiente.");
    }

    dirDestino.contenidoLista.add(copia);
    usuarioActual.espacio_usado += espacioNecesario;

    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Elemento copiado correctamente.");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar JSON.");
    }
}


    private Nodo clonarNodo(Nodo original) {
    Nodo copia = new Nodo();
    copia.nombre = original.nombre;
    copia.tipo = original.tipo;
    copia.extension = original.extension;
    copia.contenido = original.contenido;
    copia.tamano = original.tamano;
    copia.fecha_creacion = java.time.LocalDateTime.now().toString();
    copia.fecha_modificacion = copia.fecha_creacion;

    if ("directorio".equals(original.tipo)) {
        copia.contenidoLista = new ArrayList<>();
        for (Nodo hijo : original.contenidoLista) {
            Nodo hijoCopia = clonarNodo(hijo);
            copia.contenidoLista.add(hijoCopia);
        }
    }

    return copia;
}
    private CommandResponse handleMove(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login.");
    }

    String nombre = (String) command.getParameters()[0];
    String rutaOrigen = (String) command.getParameters()[1];
    String rutaDestino = (String) command.getParameters()[2];

    Nodo dirOrigen = buscarNodoPorRuta(usuarioActual.drive, rutaOrigen);
    Nodo dirDestino = buscarNodoPorRuta(usuarioActual.drive, rutaDestino);

    if (dirOrigen == null || !"directorio".equals(dirOrigen.tipo)) {
        return new CommandResponse(false, "Ruta de origen inválida.");
    }
    if (dirDestino == null || !"directorio".equals(dirDestino.tipo)) {
        return new CommandResponse(false, "Ruta de destino inválida.");
    }

    Nodo original = null;
    for (Nodo hijo : dirOrigen.contenidoLista) {
        if (hijo.nombre.trim().equalsIgnoreCase(nombre.trim())) {
            original = hijo;
            break;
        }
    }

    if (original == null) {
        return new CommandResponse(false, "Elemento a mover no encontrado.");
    }

    for (Nodo hijo : dirDestino.contenidoLista) {
        if (hijo.nombre.equals(original.nombre)) {
            return new CommandResponse(false, "Ya existe un elemento con ese nombre en el destino.");
        }
    }

    int espacio = calcularEspacio(original);
    if (usuarioActual.espacio_usado + espacio > usuarioActual.espacio_total) {
        return new CommandResponse(false, "Espacio insuficiente.");
    }

    Nodo copia = clonarNodo(original);
    copia.fecha_creacion = java.time.LocalDateTime.now().toString();
    copia.fecha_modificacion = copia.fecha_creacion;

    dirDestino.contenidoLista.add(copia);
    dirOrigen.contenidoLista.remove(original); // 💥 Aquí eliminamos el original

    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Elemento movido correctamente.");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar JSON.");
    }
}
    private CommandResponse handleShare(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login.");
    }

    // Obtener parámetros
    String nombreElemento = (String) command.getParameters()[0];
    String rutaOrigen = (String) command.getParameters()[1];
    String usuarioDestinoNombre = (String) command.getParameters()[2];

    // Buscar el nodo original desde la ruta origen
    Nodo origenDir = buscarNodoPorRuta(usuarioActual.drive, rutaOrigen);
    if (origenDir == null || !"directorio".equals(origenDir.tipo)) {
        return new CommandResponse(false, "Ruta de origen inválida.");
    }

    // Buscar el nodo a compartir dentro del directorio origen
    Nodo original = null;
    for (Nodo hijo : origenDir.contenidoLista) {
        if (hijo.nombre.equalsIgnoreCase(nombreElemento)) {
            original = hijo;
            break;
        }
    }

    if (original == null) {
        return new CommandResponse(false, "Elemento a compartir no encontrado.");
    }

    // Cargar el JSON del usuario destino
    DriveUsuario usuarioDestino;
    try {
        usuarioDestino = GestorJson.cargarDrive(usuarioDestinoNombre);
    } catch (IOException e) {
        return new CommandResponse(false, "Usuario destino no encontrado.");
    }

    // Buscar o crear la carpeta 'compartidos'
    Nodo raizDestino = usuarioDestino.drive;

    Nodo carpetaCompartidos = null;
    for (Nodo hijo : raizDestino.contenidoLista) {
        if ("compartidos".equals(hijo.nombre) && "directorio".equals(hijo.tipo)) {
            carpetaCompartidos = hijo;
            break;
        }
    }

    if (carpetaCompartidos == null) {
        carpetaCompartidos = new Nodo();
        carpetaCompartidos.nombre = "compartidos";
        carpetaCompartidos.tipo = "directorio";
        carpetaCompartidos.contenidoLista = new ArrayList<>();
        raizDestino.contenidoLista.add(carpetaCompartidos);
    }

    // Buscar o crear la subcarpeta del usuario actual
    Nodo subcarpetaUsuario = null;
    for (Nodo hijo : carpetaCompartidos.contenidoLista) {
        if (hijo.nombre.equals(nombreUsuarioActual) && "directorio".equals(hijo.tipo)) {
            subcarpetaUsuario = hijo;
            break;
        }
    }

    if (subcarpetaUsuario == null) {
        subcarpetaUsuario = new Nodo();
        subcarpetaUsuario.nombre = nombreUsuarioActual;
        subcarpetaUsuario.tipo = "directorio";
        subcarpetaUsuario.contenidoLista = new ArrayList<>();
        carpetaCompartidos.contenidoLista.add(subcarpetaUsuario);
    }

    // Verificar si ya existe un elemento con ese nombre
    for (Nodo hijo : subcarpetaUsuario.contenidoLista) {
        if (hijo.nombre.equalsIgnoreCase(original.nombre)) {
            return new CommandResponse(false, "Ya existe un elemento compartido con ese nombre.");
        }
    }

    // Clonar el nodo original
    Nodo copia = clonarNodo(original);
    copia.fecha_creacion = java.time.LocalDateTime.now().toString();
    copia.fecha_modificacion = copia.fecha_creacion;

    // Agregar la copia a la carpeta destino
    subcarpetaUsuario.contenidoLista.add(copia);

    // Guardar cambios en el JSON del usuario destino
    try {
    GestorJson.guardarDrive(usuarioDestino);
    return new CommandResponse(true, "Elemento compartido correctamente.");
} catch (IOException e) {
    return new CommandResponse(false, "Error al guardar el JSON del usuario destino.");
}

}
    private CommandResponse handleDownload(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login.");
    }

    String nombreArchivo = (String) command.getParameters()[0];
    String rutaOrigen = (String) command.getParameters()[1];

    Nodo directorio = buscarNodoPorRuta(usuarioActual.drive, rutaOrigen);
    if (directorio == null || !"directorio".equals(directorio.tipo)) {
        return new CommandResponse(false, "Ruta no encontrada.");
    }

    Nodo archivo = null;
    for (Nodo hijo : directorio.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo) && "archivo".equals(hijo.tipo)) {
            archivo = hijo;
            break;
        }
    }

    if (archivo == null) {
        return new CommandResponse(false, "Archivo no encontrado.");
    }

    return new CommandResponse(true, "Archivo listo para descargar.", archivo);
}
private CommandResponse handleLoad(Command command) {
    if (usuarioActual == null) {
        return new CommandResponse(false, "Debe hacer login.");
    }

    String localPath = (String) command.getParameters()[0];
    String pathDestino = (String) command.getParameters()[1];

    File file = new File(localPath);
    if (!file.exists() || !file.isFile()) {
        return new CommandResponse(false, "Archivo local no encontrado.");
    }

    String nombreArchivo = file.getName();
    String contenido;
    try {
        contenido = new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
        return new CommandResponse(false, "Error al leer el archivo local.");
    }

    Nodo directorio = buscarNodoPorRuta(usuarioActual.drive, pathDestino);
    if (directorio == null || !"directorio".equals(directorio.tipo)) {
        return new CommandResponse(false, "Ruta destino inválida.");
    }

    for (Nodo hijo : directorio.contenidoLista) {
        if (hijo.nombre.equals(nombreArchivo)) {
            return new CommandResponse(false, "Ya existe un archivo con ese nombre.");
        }
    }

    Nodo nuevo = new Nodo();
    nuevo.nombre = nombreArchivo;
    nuevo.tipo = "archivo";
    nuevo.contenido = contenido;
    nuevo.fechaCreacion = LocalDateTime.now().toString();

    directorio.contenidoLista.add(nuevo);

    try {
        GestorJson.guardarDrive(usuarioActual);
        return new CommandResponse(true, "Archivo cargado exitosamente.");
    } catch (IOException e) {
        return new CommandResponse(false, "Error al guardar el JSON.");
    }
}



    // Métodos para manejar cada comando específico...
}