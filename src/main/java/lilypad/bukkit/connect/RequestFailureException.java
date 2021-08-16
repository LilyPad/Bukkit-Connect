package lilypad.bukkit.connect;

public class RequestFailureException extends Exception {

    public RequestFailureException(String message) {
        super(message);
    }

    public RequestFailureException(String message, Throwable cause) {
        super(message, cause);
    }

}
