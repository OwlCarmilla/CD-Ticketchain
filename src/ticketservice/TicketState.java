package ticketservice;

import blockchain.Block;
import blockchain.BlockChain;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import templarcoin.Transaction;
import utils.Conversion;

/**
 * Estado atual dos utilizadores, saldos e bilhetes.
 *
 * Este estado não fica guardado em lado nenhum: é recalculado sempre a partir
 * dos blocos da blockchain, do primeiro ao último. A blockchain é a única
 * fonte de verdade.
 *
 * Também contém as regras que um registo novo tem de cumprir antes de ser
 * minerado (método check).
 */
public class TicketState {

    /**
     * Um bilhete e quem é o seu dono neste momento.
     */
    public static class Ticket {

        private final String id;
        private final IssueTicketsRecord issue;
        private String owner;
        private Double resalePrice; // null quando não está em revenda

        Ticket(String id, IssueTicketsRecord issue) {
            this.id = id;
            this.issue = issue;
            this.owner = issue.getSigner();
        }

        public String getId() {
            return id;
        }

        public String getEvent() {
            return issue.getEvent();
        }

        public String getOrganizer() {
            return issue.getSigner();
        }

        public String getOwner() {
            return owner;
        }

        public double getOriginalPrice() {
            return issue.getPrice();
        }

        public double getMaxResalePrice() {
            return issue.getMaxResalePrice();
        }

        /**
         * Ainda é do organizador, ou seja, ainda não foi vendido.
         */
        public boolean isUnsold() {
            return same(owner, getOrganizer());
        }

        /**
         * Preço a que pode ser comprado agora, ou null se não estiver à venda.
         * Os bilhetes por vender estão sempre à venda pelo preço original.
         */
        public Double getSalePrice() {
            return isUnsold() ? Double.valueOf(getOriginalPrice()) : resalePrice;
        }
    }

    private final Map<String, RegisterRecord> users = new HashMap<>();       // nome -> registo (papel e chave pública)
    private final Map<String, Double> balances = new HashMap<>();           // nome -> saldo em Templar Coins
    private final Map<String, IssueTicketsRecord> events = new HashMap<>(); // nome do evento -> emissão
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();      // id do bilhete -> bilhete

    /**
     * Percorre a blockchain toda e reconstrói o estado.
     * As transferências de moedas (Transaction do professor) são aplicadas tal
     * como estão; os registos de bilhetes só são aplicados se cumprirem as regras.
     */
    public static TicketState of(BlockChain bc) {
        TicketState s = new TicketState();
        for (Block b : bc.getChain()) {
            Object data;
            try {
                // o conteúdo do bloco é um objeto convertido em texto (Base64)
                data = Conversion.stringToObject(b.getData());
            } catch (Exception e) {
                continue;
            }
            if (data instanceof Transaction t) {
                s.addCoins(t.getFrom(), -t.getValue());
                s.addCoins(t.getTo(), t.getValue());
            } else if (data instanceof TicketRecord r) {
                try {
                    s.check(r);
                    s.apply(r);
                } catch (TicketException e) {
                    // um registo inválido no ficheiro é ignorado
                }
            }
        }
        return s;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     REGRAS     :::::::::::::::::::

    /**
     * Verifica se o registo pode entrar na blockchain.
     * Lança TicketException com o motivo se não cumprir alguma regra.
     */
    public void check(TicketRecord r) throws TicketException {
        // 1. a assinatura tem de ser válida (prova que foi mesmo o signatário)
        PublicKey key = signerKey(r);
        if (!r.verify(key)) {
            throw new TicketException("Assinatura inválida de " + r.getSigner());
        }
        // 2. as regras de cada tipo de registo
        switch (r) {
            case RegisterRecord reg -> checkRegister(reg);
            case IssueTicketsRecord issue -> checkIssue(issue);
            case ResaleRecord resale -> checkResale(resale);
            case BuyRecord buy -> checkBuy(buy);
            default -> throw new TicketException("Registo desconhecido");
        }
    }

    /**
     * Chave pública de quem assinou: num registo de utilizador vem no próprio
     * registo; nos outros vem do registo feito antes na blockchain.
     */
    private PublicKey signerKey(TicketRecord r) throws TicketException {
        try {
            if (r instanceof RegisterRecord reg) {
                return reg.getPublicKey();
            }
        } catch (Exception e) {
            throw new TicketException("Chave pública inválida");
        }
        RegisterRecord reg = users.get(key(r.getSigner()));
        if (reg == null) {
            throw new TicketException(r.getSigner() + " não está registado (organizador ou cliente)");
        }
        try {
            return reg.getPublicKey();
        } catch (Exception e) {
            throw new TicketException("Chave pública inválida");
        }
    }

    // cada nome só se pode registar uma vez
    private void checkRegister(RegisterRecord reg) throws TicketException {
        RegisterRecord existing = users.get(key(reg.getSigner()));
        if (existing != null) {
            throw new TicketException(reg.getSigner() + " já está registado como " + existing.getRole());
        }
        if (reg.getRole() == null) {
            throw new TicketException("Papel em falta");
        }
    }

    // só organizadores emitem bilhetes, com nome de evento único e valores válidos
    private void checkIssue(IssueTicketsRecord issue) throws TicketException {
        requireRole(issue.getSigner(), RegisterRecord.Role.ORGANIZADOR, "Só organizadores podem emitir bilhetes");
        String event = issue.getEvent();
        if (event == null || event.isBlank() || event.contains("#")) {
            throw new TicketException("Nome de evento inválido");
        }
        if (events.containsKey(key(event))) {
            throw new TicketException("Já existe um evento chamado '" + event + "'");
        }
        if (issue.getQuantity() < 1 || issue.getQuantity() > 100) {
            throw new TicketException("Número de bilhetes tem de ser entre 1 e 100");
        }
        if (issue.getPrice() <= 0) {
            throw new TicketException("O preço tem de ser maior que 0");
        }
        if (issue.getMaxResalePercent() < 0) {
            throw new TicketException("O limite de revenda não pode ser negativo");
        }
    }

    // só o dono atual pode revender, e nunca acima do limite do organizador
    private void checkResale(ResaleRecord resale) throws TicketException {
        requireRole(resale.getSigner(), RegisterRecord.Role.CLIENTE, "Só clientes podem revender bilhetes");
        Ticket t = requireTicket(resale.getTicketId());
        if (!same(t.getOwner(), resale.getSigner())) {
            throw new TicketException(resale.getSigner() + " não é o dono do bilhete " + t.getId()
                    + " (dono atual: " + t.getOwner() + ")");
        }
        if (resale.getPrice() <= 0) {
            throw new TicketException("O preço tem de ser maior que 0");
        }
        // 1e-9: margem para erros de arredondamento dos double (20 * 1.1 = 22.000000000000004)
        if (resale.getPrice() > t.getMaxResalePrice() + 1e-9) {
            throw new TicketException(String.format(
                    "Preço %.2f acima do limite de revenda: máximo %.2f (%.2f + %.0f%%)",
                    resale.getPrice(), t.getMaxResalePrice(), t.getOriginalPrice(),
                    t.issue.getMaxResalePercent()));
        }
    }

    // um cliente compra ao dono atual, pelo preço de venda, se tiver saldo
    private void checkBuy(BuyRecord buy) throws TicketException {
        requireRole(buy.getBuyer(), RegisterRecord.Role.CLIENTE, "Só clientes podem comprar bilhetes");
        Ticket t = requireTicket(buy.getTicketId());
        if (same(t.getOwner(), buy.getBuyer())) {
            throw new TicketException(buy.getBuyer() + " já é o dono do bilhete " + t.getId());
        }
        if (!same(t.getOwner(), buy.getSeller())) {
            throw new TicketException("O bilhete " + t.getId() + " já não pertence a " + buy.getSeller());
        }
        Double salePrice = t.getSalePrice();
        if (salePrice == null) {
            throw new TicketException("O bilhete " + t.getId() + " não está à venda");
        }
        if (Math.abs(salePrice - buy.getPrice()) > 1e-9) {
            throw new TicketException("O preço do bilhete " + t.getId() + " é " + salePrice);
        }
        double balance = getBalance(buy.getBuyer());
        if (balance < buy.getPrice()) {
            throw new TicketException("Saldo insuficiente: " + buy.getBuyer() + " tem " + balance
                    + ", o bilhete custa " + buy.getPrice());
        }
    }

    private void requireRole(String user, RegisterRecord.Role role, String message) throws TicketException {
        if (getRole(user) != role) {
            throw new TicketException(message);
        }
    }

    private Ticket requireTicket(String id) throws TicketException {
        Ticket t = id == null ? null : tickets.get(key(id));
        if (t == null) {
            throw new TicketException("O bilhete " + id + " não existe");
        }
        return t;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     APLICAR (registo já verificado)     :::::::::::::::::::

    /**
     * Atualiza o estado com um registo que já passou as regras.
     */
    private void apply(TicketRecord r) {
        switch (r) {
            case RegisterRecord reg ->
                users.put(key(reg.getSigner()), reg);
            case IssueTicketsRecord issue -> {
                // cria os bilhetes do evento, todos na posse do organizador
                events.put(key(issue.getEvent()), issue);
                for (int i = 0; i < issue.getQuantity(); i++) {
                    String id = issue.ticketId(i);
                    tickets.put(key(id), new Ticket(id, issue));
                }
            }
            case ResaleRecord resale ->
                tickets.get(key(resale.getTicketId())).resalePrice = resale.getPrice();
            case BuyRecord buy -> {
                // as moedas passam do comprador para o vendedor e o bilhete muda de dono
                Ticket t = tickets.get(key(buy.getTicketId()));
                addCoins(buy.getBuyer(), -buy.getPrice());
                addCoins(buy.getSeller(), buy.getPrice());
                t.owner = buy.getBuyer();
                t.resalePrice = null;
            }
            default -> {
            }
        }
    }

    private void addCoins(String user, double value) {
        balances.merge(key(user), value, Double::sum);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     CONSULTAS     :::::::::::::::::::

    public double getBalance(String user) {
        return balances.getOrDefault(key(user), 0.0);
    }

    /**
     * Papel do utilizador, ou null se ainda não estiver registado.
     */
    public RegisterRecord.Role getRole(String user) {
        RegisterRecord reg = users.get(key(user));
        return reg == null ? null : reg.getRole();
    }

    public List<Ticket> getTickets() {
        return new ArrayList<>(tickets.values());
    }

    public List<Ticket> getTicketsOwnedBy(String user) {
        List<Ticket> list = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (same(t.getOwner(), user)) {
                list.add(t);
            }
        }
        return list;
    }

    public List<Ticket> getTicketsIssuedBy(String organizer) {
        List<Ticket> list = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (same(t.getOrganizer(), organizer)) {
                list.add(t);
            }
        }
        return list;
    }

    public List<Ticket> getTicketsForSale() {
        List<Ticket> list = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (t.getSalePrice() != null) {
                list.add(t);
            }
        }
        return list;
    }

    // os nomes são comparados sem distinguir maiúsculas, como no TemplarServices do professor
    private static String key(String name) {
        return name == null ? "" : name.toLowerCase();
    }

    private static boolean same(String a, String b) {
        return key(a).equals(key(b));
    }
}
