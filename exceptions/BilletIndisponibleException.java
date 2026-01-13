package exceptions;

public class BilletIndisponibleException extends Exception {
    public BilletIndisponibleException(String message) {
        super(message);
    }
}