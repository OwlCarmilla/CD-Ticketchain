package ticketservice;

/**
 * O dono atual de um bilhete põe-no à venda (revenda) por um preço.
 *
 * A blockchain só aceita se quem assina for mesmo o dono e se o preço
 * respeitar o limite de revenda definido pelo organizador.
 */
public class ResaleRecord extends TicketRecord {

    private final String ticketId;
    private final double price;

    public ResaleRecord(String seller, String ticketId, double price) {
        super(seller);
        this.ticketId = ticketId;
        this.price = price;
    }

    public String getTicketId() {
        return ticketId;
    }

    public double getPrice() {
        return price;
    }

    @Override
    protected String content() {
        return ticketId + "|" + price;
    }

    @Override
    public String toString() {
        return "Revenda: " + getSigner() + " põe " + ticketId + " à venda por " + price;
    }

    private static final long serialVersionUID = 1L;
}
