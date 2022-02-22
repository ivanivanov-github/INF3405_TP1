package CustomExceptions;

public class InvalidPasswordException extends Exception {
    public InvalidPasswordException() {
        super("Erreur dans la saisie du mot de passe");
    }
}
