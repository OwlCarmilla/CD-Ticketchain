package ticketservice;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import utils.SecurityUtils;

/**
 * Base class for every ticket record stored in a block.
 * Each record is signed by the user who creates it.
 */
public abstract class TicketRecord implements Serializable {

    private final String signer;
    private String signature;

    protected TicketRecord(String signer) {
        this.signer = signer;
    }

    /**
     * The fields that are signed, one record type at a time.
     */
    protected abstract String content();

    // the record type is part of the signed data, so a signature cannot be reused on another type
    private byte[] signedData() {
        return (getClass().getSimpleName() + "|" + signer + "|" + content()).getBytes(StandardCharsets.UTF_8);
    }

    public void sign(PrivateKey key) throws Exception {
        signature = Base64.getEncoder().encodeToString(SecurityUtils.sign(signedData(), key));
    }

    public boolean verify(PublicKey key) {
        try {
            return signature != null
                    && SecurityUtils.verifySign(signedData(), Base64.getDecoder().decode(signature), key);
        } catch (Exception e) {
            return false;
        }
    }

    public String getSigner() {
        return signer;
    }

    private static final long serialVersionUID = 1L;
}
