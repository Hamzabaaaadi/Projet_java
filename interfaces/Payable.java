package interfaces;

import model.*;
import exceptions.*;

public interface Payable {
    Paiement payer(Billet billet, String modePaiement) throws PaiementInvalideException;
    boolean verifierPaiement(Billet billet);
}