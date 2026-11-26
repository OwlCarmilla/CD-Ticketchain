package ticketservice;

/**
 * Um cliente compra um bilhete ao seu dono atual: ao organizador (pelo preço
 * original) ou a outro cliente que o pôs em revenda.
 *
 * É assinado pelo comprador, que assim aceita pagar o preço indicado.
 * As moedas e o bilhete mudam de dono no mesmo registo, por isso uma compra
 * nunca fica a meio.
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
