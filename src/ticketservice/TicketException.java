package ticketservice;

/**
 * Lançada quando um registo de bilhetes não cumpre uma regra.
 * A mensagem explica o motivo e é mostrada ao utilizador.
 */
public class TicketException extends Exception {

    public TicketException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;
}
