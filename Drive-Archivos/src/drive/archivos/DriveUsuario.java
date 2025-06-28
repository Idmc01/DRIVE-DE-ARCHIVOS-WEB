package drive.archivos;

import java.io.Serializable;

public class DriveUsuario implements Serializable {
    public String usuario;
    public int espacio_total;
    public int espacio_usado;
    public String ruta_actual;
    public Nodo drive;
    public Nodo compartidos;
}

