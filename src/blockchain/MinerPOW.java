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

package blockchain;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import templarcoin.TemplarServices;

/**
 * Created on 07/10/2026, 17:32:15
 *
 * @author manso - computer
 */
public class MinerPOW {

    public static class MinerThr extends Thread {

        AtomicInteger nonce;
        AtomicInteger counter;
        String data;
        int dif;

        public MinerThr(AtomicInteger nonce, AtomicInteger counter, String data, int dif) {
            this.nonce = nonce;
            this.counter = counter;
            this.data = data;
            this.dif = dif;
        }

        public void run() {
            String zeros = String.format("%0" + dif + "d", 0);
            while (nonce.get() == 0) {
                int n = counter.getAndIncrement();
                String hash = utils.SecurityUtils.getHash(data + n);

                if (hash.startsWith(zeros)) {
                    System.out.println(n + " " + hash);
                    nonce.set(n);
                }
            }
        }
    }
     public static int getNonceP(String data, int dif) {
         int cores = Runtime.getRuntime().availableProcessors();
         MinerThr thr[] = new MinerThr[cores];
         AtomicInteger counter = new AtomicInteger();
         AtomicInteger nonce = new AtomicInteger();
         for (int i = 0; i < thr.length; i++) {
             thr[i] = new MinerThr(nonce, counter, data, dif);
             thr[i].start();  
         }
        try {
            thr[0].join();
        } catch (InterruptedException ex) {
            System.getLogger(MinerPOW.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
         return nonce.get();
     }

    public static int getNonce(String data, int dif) {
        String zeros = String.format("%0" + dif + "d", 0);
        int counter = 0;
        while (true) {
            String hash = utils.SecurityUtils.getHash(data + counter);
            if (hash.startsWith(zeros)) {
                System.out.println(counter + " " + hash);
                return counter;
            }
            counter++;
        }
    }

    public static String getSigner(BlockChain bc) throws Exception {
        Map<String, Double> signers = TemplarServices.getUsersCoins(bc);
        double total = 0;
        for (Double d : signers.values()) {
            total += d;
        }
        double selectedValue = Math.random() * total;
        double acumulated = 0;
        for (String user : signers.keySet()) {
            acumulated += signers.get(user);
            if (acumulated >= selectedValue) {
                return user;
            }
        }
        return "System";
    }
}
