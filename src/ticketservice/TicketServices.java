package ticketservice;

import blockchain.BlockChain;
import utils.Conversion;

/**
 * Adiciona registos de bilhetes à blockchain, só depois de passarem as regras.
 */
public class TicketServices {

    /**
     * Verifica o registo (já assinado) contra a versão mais recente da
     * blockchain e, se for válido, mina um novo bloco com ele.
     *
     * @throws TicketException se o registo não cumprir uma regra (nada é minerado)
     */
    public static void submit(BlockChain bc, TicketRecord record) throws Exception {
        synchronized (bc) {
            // lê a cadeia do ficheiro: outras janelas podem ter adicionado blocos
            bc.reload();
            // recalcula o estado a partir dos blocos e verifica as regras
            TicketState.of(bc).check(record);
            // converte o registo em texto e mina o bloco (proof-of-work)
            bc.addData(Conversion.objectToString(record));
        }
    }
}
