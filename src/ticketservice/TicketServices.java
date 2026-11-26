package ticketservice;

import blockchain.BlockChain;
import utils.Conversion;

/**
 * Adds ticket records to the blockchain, only after they pass the rules.
 */
public class TicketServices {

    /**
     * Checks the (already signed) record against the latest chain and mines it.
     *
     * @throws TicketException if the record breaks a rule; nothing is mined
     */
    public static void submit(BlockChain bc, TicketRecord record) throws Exception {
        synchronized (bc) {
            bc.reload();
            TicketState.of(bc).check(record);
            bc.addData(Conversion.objectToString(record));
        }
    }
}
