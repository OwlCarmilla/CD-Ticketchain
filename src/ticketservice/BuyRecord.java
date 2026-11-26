package ticketservice;

/**
 * A client buys a ticket from its current owner (the organizer, or a client
 * who put it up for resale). The coins and the ticket move together.
 */
public class BuyRecord extends TicketRecord {

    private final String seller;
    private final String ticketId;
    private final double price;

    public BuyRecord(String buyer, String seller, String ticketId, double price) {
        super(buyer);
        this.seller = seller;
        this.ticketId = ticketId;
        this.price = price;
    }

    public String getBuyer() {
        return getSigner();
    }

    public String getSeller() {
        return seller;
    }

    public String getTicketId() {
        return ticketId;
    }

    public double getPrice() {
        return price;
    }

    @Override
    protected String content() {
        return seller + "|" + ticketId + "|" + price;
    }

    @Override
    public String toString() {
        return "Compra: " + getSigner() + " compra " + ticketId + " a " + seller + " por " + price;
    }

    private static final long serialVersionUID = 1L;
}
