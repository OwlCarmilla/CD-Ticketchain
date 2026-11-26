//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: 
//::                                                                         ::
//::     Antonio Manuel Rodrigues Manso                                      ::
//::                                                                         ::
//::     I N S T I T U T O    P O L I T E C N I C O   D E   T O M A R        ::
//::     Escola Superior de Tecnologia de Tomar                              ::
//::     e-mail: manso@ipt.pt                                                ::
//::     url   : http://orion.ipt.pt/~manso                                  ::
//::                                                                         ::
//::     This software was build with the purpose of investigate and         ::
//::     learning.                                                           ::
//::                                                                         ::
//::                                                               (c)2026   ::
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 //////////////////////////////////////////////////////////////////////////////

package templarcoin;

import blockchain.Block;
import blockchain.BlockChain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created on 07/10/2026, 18:16:43
 *
 * @author manso - computer
 */
public class TemplarServices {

    //devolve o saldo do utilizador (transações + compras/vendas de bilhetes)
    public static double getAmount(String user, BlockChain bc) throws Exception {
        return ticketservice.TicketState.of(bc).getBalance(user);
    }

    public static List<String> getUsers(BlockChain bc) throws Exception {
        TreeSet<String> users = new TreeSet<>();
        for (Block b : bc.getChain()) {
            try {
                Transaction t = utils.Conversion.stringToObject(b.getData());
                users.add(t.getFrom());
                users.add(t.getTo());
            } catch (Exception e) {
            }
        }
        return new ArrayList<>(users);
    }
    public static Map<String,Double> getUsersCoins(BlockChain bc) throws Exception {
        List<String> users = getUsers(bc);        
        users.remove("MASTER");
        users.remove("System");
        HashMap<String,Double> coins = new HashMap<>();
        
        for (String user : users) {
            coins.put(user, getAmount(user, bc));
        }
        return coins;
    }

   

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202610071816L;
    //:::::::::::::::::::::::::::  Copyright(c) M@nso  2026  :::::::::::::::::::


///////////////////////////////////////////////////////////////////////////
}
