package CustomExceptions;

public class InvalidIPAddressAndPortException extends Exception {
    public InvalidIPAddressAndPortException() {
        super("Aucun server ecoute sur l'adresse IP et port donnees");
    }
}
