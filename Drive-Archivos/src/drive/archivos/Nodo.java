package drive.archivos;

import java.io.Serializable;
import java.util.*;

public class Nodo implements Serializable {
    public String nombre;
    public String tipo; // "archivo" o "directorio"
    public String extension; // solo para archivos
    public String contenido; // solo para archivos
    public int tamano;       // solo para archivos
    public String fecha_creacion;
    public String fecha_modificacion;
    public List<Nodo> contenidoLista; // solo para directorios
    
     public String fechaCreacion;

    public Nodo() {
        contenidoLista = new ArrayList<>();
    }
}

