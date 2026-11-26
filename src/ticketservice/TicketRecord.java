package ticketservice;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import utils.SecurityUtils;

/**
 * Classe base de todos os registos de bilhetes guardados nos blocos.
 *
 * Cada registo é assinado com a chave privada de quem o cria. Antes de ser
 * minerado, a assinatura é verificada com a chave pública desse utilizador,
 * que ficou registada na blockchain pelo RegisterRecord.
 */
public abstract class TicketRecord implements Serializable {

    private final String signer;  // quem cria e assina o registo
    private String signature;     // assinatura RSA em Base64

    protected TicketRecord(String signer) {
        this.signer = signer;
    }

    /**
     * Os campos de cada tipo de registo que entram na assinatura.
     */
    protected abstract String content();

    // o tipo do registo também é assinado, para que uma assinatura
    // não possa ser reaproveitada noutro tipo de registo
    private byte[] signedData() {
        return (getClass().getSimpleName() + "|" + signer + "|" + content()).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Assina o registo com a chave privada de quem o cria.
     */
    public void sign(PrivateKey key) throws Exception {
        signature = Base64.getEncoder().encodeToString(SecurityUtils.sign(signedData(), key));
    }

    /**
     * Verifica a assinatura com a chave pública do signatário.
     */
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
