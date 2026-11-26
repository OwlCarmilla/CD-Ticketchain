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

import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import utils.Conversion;
import utils.SecurityUtils;

/**
 * Created on 07/10/2026, 17:59:06
 *
 * @author manso - computer
 */
public class Transaction implements Serializable {

    String from;
    String to;
    double value;
    String pkFrom;
    String signature;

    public Transaction(String from, String to, double value) {
        this.from = from;
        this.to = to;
        this.value = value;
    }

    public Transaction(String from, String to, double value, PrivateKey key) throws Exception {
        this.from = from;
        this.to = to;
        this.value = value;
        PublicKey pub = SecurityUtils.loadPublicKey(from);
        this.pkFrom = Conversion.objectToString(pub);
        String data = from + to + value;
        byte[] sign = SecurityUtils.sign(data.getBytes(), key);        
        this.signature = Conversion.objectToString(sign);
    }

    @Override
    public String toString() {
        boolean isOK = false;
        try {
            byte[] sign = Conversion.stringToObject(this.signature);
            PublicKey pub = Conversion.stringToObject(pkFrom);
            String data = from + to + value;
            isOK = SecurityUtils.verifySign( data.getBytes(), sign,pub);
        } catch (Exception ex) {
            System.getLogger(Transaction.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return "Transaction{" + "from=" + from + ", to=" + to + ", value=" + value + '}' + "signature : " + isOK;

    }

    public String toBase64() throws IOException {
        return utils.Conversion.objectToString(this);
    }

    public static Transaction fromBase64(String txt) throws Exception {
        return (Transaction) utils.Conversion.stringToObject(txt);
    }

    public String getFrom() {
        return from;
    }

    public String getPkFrom() {
        return this.pkFrom;
    }

    public String getTo() {
        return to;
    }

    public double getValue() {
        return value;
    }

    public String getSignature() {
        return this.signature;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202610071759L;
    //:::::::::::::::::::::::::::  Copyright(c) M@nso  2026  :::::::::::::::::::


///////////////////////////////////////////////////////////////////////////
}
