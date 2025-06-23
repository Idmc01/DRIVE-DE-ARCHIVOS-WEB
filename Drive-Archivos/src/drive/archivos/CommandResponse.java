package drive.archivos;

import java.io.Serializable;

public class CommandResponse implements Serializable {
    private boolean success;
    private String message;
    private Object data;

    public CommandResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters y setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
