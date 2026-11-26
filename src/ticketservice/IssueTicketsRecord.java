package ticketservice;

/**
 * Um organizador emite um número de bilhetes para um evento, com um preço e um
 * limite de revenda (por exemplo +10% do preço inicial).
 *
 * Os bilhetes ficam com os nomes "evento#0", "evento#1", ... e começam todos
 * na posse do organizador, à venda pelo preço original.
 */
public class IssueTicketsRecord extends TicketRecord {

    private final String event;
    private final int quantity;
    private final double price;
    private final double maxResalePercent; // aumento máximo permitido na revenda, em %

    public IssueTicketsRecord(String organizer, String event, int quantity, double price, double maxResalePercent) {
        super(organizer);
        this.event = event;
        this.quantity = quantity;
        this.price = price;
        this.maxResalePercent = maxResalePercent;
    }

    public String getEvent() {
        return event;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getMaxResalePercent() {
        return maxResalePercent;
    }

    /**
     * Preço máximo de revenda: preço * (1 + limite / 100).
     */
    public double getMaxResalePrice() {
        return price * (1 + maxResalePercent / 100);
    }

    /**
     * Identificador do bilhete número i deste evento.
     */
    public String ticketId(int i) {
        return event + "#" + i;
    }

    @Override
    protected String content() {
        return event + "|" + quantity + "|" + price + "|" + maxResalePercent;
    }

    @Override
    public String toString() {
        return "Emissão: " + getSigner() + " emite " + quantity + " bilhetes para '" + event
                + "' a " + price + " (revenda até +" + maxResalePercent + "%)";
    }

    private static final long serialVersionUID = 1L;
}
