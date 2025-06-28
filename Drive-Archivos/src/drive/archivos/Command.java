package drive.archivos;
import java.io.Serializable;

public class Command implements Serializable {
    private CommandType type;
    private Object[] parameters;

    public Command(CommandType type, Object... parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    // Getters
    public CommandType getType() { return type; }
    public Object[] getParameters() { return parameters; }
}

enum CommandType {
    CREATE_DRIVE, LOGIN, CREATE_FILE, CREATE_DIR,
    CHANGE_DIR, LIST_DIR, MODIFY_FILE, VIEW_PROPERTIES,
    VIEW_FILE, COPY, MOVE, LOAD, DOWNLOAD, DELETE, SHARE
}