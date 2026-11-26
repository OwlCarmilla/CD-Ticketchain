package ticketservice;

/**
 * A ticket record that breaks a rule, with the reason shown to the user.
 */
public class TicketException extends Exception {

    public TicketException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;
}
