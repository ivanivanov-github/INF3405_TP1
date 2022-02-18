package CustomExceptions;

public class UserNotInDataBaseException extends Exception {
    public UserNotInDataBaseException() {
        super("User not in database");
    }
}
