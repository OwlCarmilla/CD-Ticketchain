/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ticketservice;

import blockchain.BlockChain;

/**
 *
 * @author Catarina Trabalho
 */
public class CD_Ticketchain {

    /**
     * Loads the blockchain (creating the genesis block on first run) and prints it.
     */
    public static void main(String[] args) throws Exception {
        BlockChain bc = new BlockChain();
        System.out.println("Blocks: " + bc.getChain().size());
        System.out.println(bc);
    }

}
