package CustomExceptions;

public class UserIsAlreadyConnectedException extends Exception {
    public UserIsAlreadyConnectedException() {
        super("Client is already connected with this account");
    }
}
