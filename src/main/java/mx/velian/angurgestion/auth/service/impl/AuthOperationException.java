package mx.velian.angurgestion.auth.service.impl;

public class AuthOperationException extends RuntimeException {
    public AuthOperationException(String message) {
        super(message);
    }

    public AuthOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
