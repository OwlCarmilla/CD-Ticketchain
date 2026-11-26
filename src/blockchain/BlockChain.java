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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import templarcoin.Transaction;
import utils.SecurityUtils;

/**
 * Created on 07/10/2026, 17:45:54
 *
 * @author manso - computer
 */
public class BlockChain {
    
    public static  int DIFICULTY = 3;

    public static final String FILENAME = "blockchain.bc";

    ArrayList<Block> chain = new ArrayList<>();

    public BlockChain() throws IOException {
        //read blockchain from file
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME))) {
            chain = (ArrayList<Block>) in.readObject();
        } catch (Exception e) {
            reset();
        }
    }

    public void reset() {
        try {
            //creates new blockchain
            String prev = "000000";
            // first run on a fresh checkout: key files are not in git, so create MASTER's
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get("MASTER.pub"))) {
                java.security.KeyPair kp = SecurityUtils.generateKeyPair(2048);
                SecurityUtils.savePublicKey(kp.getPublic(), "MASTER");
                SecurityUtils.savePrivateKey(kp.getPrivate(), "MASTER", "123qwe");
            }
            PrivateKey priv = SecurityUtils.loadPrivateKey("MASTER", "123qwe");
            Transaction genesis = new Transaction("MASTER", "System", 1000, priv);
            System.out.println("Genesis " + genesis.toString());
            String data = genesis.toBase64();     
            int nonce = MinerPOW.getNonceP(prev + data + DIFICULTY, DIFICULTY);
            String current = utils.SecurityUtils.getHash(prev + data + DIFICULTY + nonce);                        
            Block b = new Block(prev, data, nonce, DIFICULTY, current);
            chain.add(b);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create genesis block", ex);
        }
    }

    /**
     * Re-reads the chain from the file, so blocks mined by other open windows
     * are not lost. Keeps the current chain if the file does not exist yet.
     */
    public synchronized void reload() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME))) {
            chain = (ArrayList<Block>) in.readObject();
        } catch (Exception e) {
        }
    }

    public synchronized void addData(String data) {
        reload();
        String prev = chain.getLast().getCurrentHash();
        int nonce = MinerPOW.getNonceP(prev + data + DIFICULTY, DIFICULTY);
        String current = utils.SecurityUtils.getHash(prev + data + DIFICULTY + nonce);
        Block b = new Block(prev, data, nonce, DIFICULTY, current);
        chain.add(b);
        //save data
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            out.writeObject(chain);
        } catch (Exception e) {
        }
    }

    public Block getLastBlock() {
        return chain.getLast();
    }

    public String toString() {
        StringBuilder txt = new StringBuilder();
        for (Block block : chain) {
            txt.append("\n").append(block.toString());
        }
        return txt.toString();
    }

    public ArrayList<Block> getChain() {
        return chain;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202610071745L;
    //:::::::::::::::::::::::::::  Copyright(c) M@nso  2026  :::::::::::::::::::


///////////////////////////////////////////////////////////////////////////
}
