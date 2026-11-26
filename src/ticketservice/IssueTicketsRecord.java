package ticketservice;

/**
 * An organizer issues a number of tickets for an event, with a price and a
 * maximum resale increase. Tickets are named "event#0", "event#1", ...
 */
public class IssueTicketsRecord extends TicketRecord {

    private final String event;
    private final int quantity;
    private final double price;
    private final double maxResalePercent;

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

    public double getMaxResalePrice() {
        return price * (1 + maxResalePercent / 100);
    }

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
