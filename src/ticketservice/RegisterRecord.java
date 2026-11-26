package ticketservice;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Regista um utilizador na blockchain com o seu papel (organizador ou cliente)
 * e a sua chave pública. É daqui que vem a chave pública usada para verificar
 * as assinaturas de todos os registos seguintes desse utilizador.
 */
public class RegisterRecord extends TicketRecord {

    /**
     * Os dois tipos de utilizador do sistema.
     */
    public enum Role {
        ORGANIZADOR, CLIENTE
    }

    private final Role role;
    private final String publicKey; // chave pública em Base64 (formato X.509)

    public RegisterRecord(String name, Role role, PublicKey publicKey) {
        super(name);
        this.role = role;
        this.publicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public Role getRole() {
        return role;
    }

    /**
     * Reconstrói a chave pública a partir do texto em Base64.
     */
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
