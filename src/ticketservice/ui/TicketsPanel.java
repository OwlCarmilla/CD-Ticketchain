package ticketservice.ui;

import blockchain.BlockChain;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import ticketservice.BuyRecord;
import ticketservice.IssueTicketsRecord;
import ticketservice.RegisterRecord;
import ticketservice.ResaleRecord;
import ticketservice.TicketException;
import ticketservice.TicketRecord;
import ticketservice.TicketServices;
import ticketservice.TicketState;
import ticketservice.TicketState.Ticket;

/**
 * Separador "Bilhetes" da carteira.
 *
 * Mostra uma de três vistas, conforme o papel do utilizador na blockchain:
 * - sem papel: botões para se registar como organizador ou cliente;
 * - organizador: emitir bilhetes e ver quem é dono de cada um;
 * - cliente: comprar bilhetes e revender os seus.
 */
public class TicketsPanel extends JPanel {

    private final String user;
    private final PrivateKey privKey;
    private final PublicKey pubKey;
    private final BlockChain blockchain;
    private final Runnable onChange; // atualiza o resto da carteira (saldo, lista de blocos)

    private final JLabel lblInfo = new JLabel();
    private final JLabel lblWorking = new JLabel("A minerar bloco...");
    private final CardLayout cards = new CardLayout();        // troca entre as três vistas
    private final JPanel cardPanel = new JPanel(cards);
    private final List<JButton> buttons = new ArrayList<>(); // desativados enquanto se mina

    // vista do organizador
    private final JTextField txtEvent = new JTextField("Concerto");
    private final JTextField txtQuantity = new JTextField("5");
    private final JTextField txtPrice = new JTextField("20");
    private final JTextField txtMaxResale = new JTextField("10");
    private final JTextArea txtIssued = new JTextArea();

    // vista do cliente (cada lista tem a lista de bilhetes correspondente às linhas)
    private final DefaultListModel<String> forSaleModel = new DefaultListModel<>();
    private final JList<String> lstForSale = new JList<>(forSaleModel);
    private List<Ticket> forSale = new ArrayList<>();
    private final DefaultListModel<String> mineModel = new DefaultListModel<>();
    private final JList<String> lstMine = new JList<>(mineModel);
    private List<Ticket> mine = new ArrayList<>();
    private final JTextField txtResaleTicket = new JTextField(12);
    private final JTextField txtResalePrice = new JTextField(6);

    public TicketsPanel(String user, PrivateKey privKey, PublicKey pubKey, BlockChain blockchain, Runnable onChange) {
        this.user = user;
        this.privKey = privKey;
        this.pubKey = pubKey;
        this.blockchain = blockchain;
        this.onChange = onChange;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.add(lblInfo, BorderLayout.CENTER);
        JButton btRefresh = new JButton("Atualizar");
        btRefresh.addActionListener(e -> refresh());
        top.add(btRefresh, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        cardPanel.add(buildRegisterCard(), "none");
        cardPanel.add(buildOrganizerCard(), RegisterRecord.Role.ORGANIZADOR.name());
        cardPanel.add(buildClientCard(), RegisterRecord.Role.CLIENTE.name());
        add(cardPanel, BorderLayout.CENTER);

        lblWorking.setVisible(false);
        add(lblWorking, BorderLayout.SOUTH);

        refresh();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     ECRÃS     :::::::::::::::::::

    private JPanel buildRegisterCard() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel label = new JLabel("Ainda não tem papel na blockchain. Registe-se como:", JLabel.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(p, BorderLayout.CENTER);
        p.add(button("Registar como Organizador",
                () -> submit(new RegisterRecord(user, RegisterRecord.Role.ORGANIZADOR, pubKey),
                        "Registado como organizador")));
        p.add(button("Registar como Cliente",
                () -> submit(new RegisterRecord(user, RegisterRecord.Role.CLIENTE, pubKey),
                        "Registado como cliente")));
        return wrapper;
    }

    private JPanel buildOrganizerCard() {
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Emitir bilhetes"));
        form.add(new JLabel("Evento"));
        form.add(txtEvent);
        form.add(new JLabel("Número de bilhetes"));
        form.add(txtQuantity);
        form.add(new JLabel("Preço (TC)"));
        form.add(txtPrice);
        form.add(new JLabel("Limite de revenda (+%)"));
        form.add(txtMaxResale);
        form.add(new JLabel());
        form.add(button("Emitir bilhetes", this::issueTickets));

        txtIssued.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtIssued);
        scroll.setBorder(BorderFactory.createTitledBorder("Os meus bilhetes emitidos"));

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(form, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildClientCard() {
        lstForSale.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel sale = new JPanel(new BorderLayout(5, 5));
        sale.setBorder(BorderFactory.createTitledBorder("Bilhetes à venda"));
        sale.add(new JScrollPane(lstForSale), BorderLayout.CENTER);
        JPanel saleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saleButtons.add(button("Comprar", this::buySelected));
        sale.add(saleButtons, BorderLayout.SOUTH);

        lstMine.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // ao escolher um bilhete, preenche a revenda com o preço máximo permitido
        lstMine.addListSelectionListener(e -> {
            int i = lstMine.getSelectedIndex();
            if (!e.getValueIsAdjusting() && i >= 0 && i < mine.size()) {
                Ticket t = mine.get(i);
                txtResaleTicket.setText(t.getId());
                txtResalePrice.setText(String.valueOf(round(t.getMaxResalePrice())));
            }
        });
        JPanel owned = new JPanel(new BorderLayout(5, 5));
        owned.setBorder(BorderFactory.createTitledBorder("Os meus bilhetes"));
        owned.add(new JScrollPane(lstMine), BorderLayout.CENTER);
        JPanel resale = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        resale.add(new JLabel("Bilhete"));
        resale.add(txtResaleTicket);
        resale.add(new JLabel("Preço"));
        resale.add(txtResalePrice);
        resale.add(button("Revender", this::resell));
        owned.add(resale, BorderLayout.SOUTH);

        JPanel p = new JPanel(new GridLayout(2, 1, 5, 5));
        p.add(sale);
        p.add(owned);
        return p;
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        buttons.add(b);
        return b;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     AÇÕES     :::::::::::::::::::

    private void issueTickets() {
        try {
            int quantity = Integer.parseInt(txtQuantity.getText().trim());
            double price = Double.parseDouble(txtPrice.getText().trim());
            double maxResale = Double.parseDouble(txtMaxResale.getText().trim());
            String event = txtEvent.getText().trim();
            submit(new IssueTicketsRecord(user, event, quantity, price, maxResale),
                    "Emitidos " + quantity + " bilhetes para '" + event + "'");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Número inválido", "Emitir bilhetes", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void buySelected() {
        int i = lstForSale.getSelectedIndex();
        if (i < 0 || i >= forSale.size()) {
            JOptionPane.showMessageDialog(this, "Escolha um bilhete da lista", "Comprar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Ticket t = forSale.get(i);
        // compra ao dono atual, pelo preço a que está à venda
        submit(new BuyRecord(user, t.getOwner(), t.getId(), t.getSalePrice()),
                "Comprou " + t.getId() + " por " + t.getSalePrice());
    }

    // o id do bilhete é escrito à mão, para se poder mostrar que a blockchain
    // rejeita a revenda de um bilhete que já não é nosso
    private void resell() {
        try {
            String ticketId = txtResaleTicket.getText().trim();
            double price = Double.parseDouble(txtResalePrice.getText().trim());
            submit(new ResaleRecord(user, ticketId, price), ticketId + " posto à venda por " + price);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Preço inválido", "Revender", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Assina o registo, verifica as regras e mina o bloco numa thread à parte,
     * para não bloquear a interface. No fim mostra se foi aceite ou rejeitado.
     */
    private void submit(TicketRecord record, String successMessage) {
        setBusy(true);
        new Thread(() -> {
            String error = null;
            try {
                record.sign(privKey);
                TicketServices.submit(blockchain, record);
            } catch (TicketException e) {
                error = e.getMessage();
            } catch (Exception e) {
                error = "Erro: " + e.getMessage();
            }
            String result = error;
            // a interface só pode ser alterada na thread do Swing
            SwingUtilities.invokeLater(() -> {
                setBusy(false);
                if (result == null) {
                    JOptionPane.showMessageDialog(this, successMessage, "Aceite pela blockchain",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, result, "Rejeitado pela blockchain",
                            JOptionPane.ERROR_MESSAGE);
                }
                refresh();
                onChange.run();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        lblWorking.setVisible(busy);
        for (JButton b : buttons) {
            b.setEnabled(!busy);
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::     ATUALIZAR     :::::::::::::::::::

    /**
     * Volta a ler a blockchain do ficheiro (outras janelas podem ter
     * adicionado blocos), recalcula o estado e redesenha o separador.
     */
    public void refresh() {
        new Thread(() -> {
            blockchain.reload();
            TicketState state = TicketState.of(blockchain);
            SwingUtilities.invokeLater(() -> show(state));
        }).start();
    }

    /**
     * Mostra o estado: papel e saldo no topo, e a vista certa para o papel.
     */
    private void show(TicketState state) {
        RegisterRecord.Role role = state.getRole(user);
        lblInfo.setText("<html><b>" + user + "</b> · Papel: "
                + (role == null ? "nenhum" : role) + " · Saldo: " + round(state.getBalance(user)) + " TC</html>");
        cards.show(cardPanel, role == null ? "none" : role.name());

        // organizador: os bilhetes que emitiu e o dono atual de cada um
        StringBuilder issued = new StringBuilder();
        for (Ticket t : state.getTicketsIssuedBy(user)) {
            issued.append(t.getId()).append("   dono: ").append(t.getOwner());
            if (t.isUnsold()) {
                issued.append("   (por vender, ").append(round(t.getOriginalPrice())).append(" TC)");
            } else if (t.getSalePrice() != null) {
                issued.append("   (em revenda por ").append(round(t.getSalePrice())).append(" TC)");
            }
            issued.append("\n");
        }
        txtIssued.setText(issued.toString());

        // cliente: bilhetes à venda (do organizador ou em revenda), exceto os seus
        forSale = new ArrayList<>();
        forSaleModel.clear();
        for (Ticket t : state.getTicketsForSale()) {
            if (!t.getOwner().equalsIgnoreCase(user)) {
                forSale.add(t);
                forSaleModel.addElement(t.getId() + "   " + round(t.getSalePrice()) + " TC   vendedor: "
                        + t.getOwner() + (t.isUnsold() ? " (organizador)" : " (revenda)"));
            }
        }

        // cliente: os bilhetes que tem
        mine = state.getTicketsOwnedBy(user);
        mineModel.clear();
        for (Ticket t : mine) {
            mineModel.addElement(t.getId() + "   (original " + round(t.getOriginalPrice())
                    + " TC, revenda até " + round(t.getMaxResalePrice()) + " TC)"
                    + (t.getSalePrice() != null ? "   à venda por " + round(t.getSalePrice()) + " TC" : ""));
        }
    }

    // arredonda a 2 casas decimais para mostrar
    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static final long serialVersionUID = 1L;
}
