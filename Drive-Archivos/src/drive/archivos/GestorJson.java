package drive.archivos;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class GestorJson {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String BASE_PATH = "users_data/";

    public static DriveUsuario cargarDrive(String usuario) throws IOException {
        Path path = Paths.get(BASE_PATH + usuario + ".json");
        if (!Files.exists(path)) return null;
        String json = new String(Files.readAllBytes(path));
        return gson.fromJson(json, DriveUsuario.class);
    }

    public static void guardarDrive(DriveUsuario drive) throws IOException {
        String json = gson.toJson(drive);
        Files.write(Paths.get(BASE_PATH + drive.usuario + ".json"), json.getBytes());
    }
}

