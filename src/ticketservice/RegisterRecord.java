package ticketservice;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Registers a user with a role and their public key.
 */
public class RegisterRecord extends TicketRecord {

    public enum Role {
        ORGANIZADOR, CLIENTE
    }

    private final Role role;
    private final String publicKey;

    public RegisterRecord(String name, Role role, PublicKey publicKey) {
        super(name);
        this.role = role;
        this.publicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public Role getRole() {
        return role;
    }

    public PublicKey getPublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKey);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    @Override
    protected String content() {
        return role + "|" + publicKey;
    }

    @Override
    public String toString() {
        return "Registo: " + getSigner() + " como " + role;
    }

    private static final long serialVersionUID = 1L;
}
