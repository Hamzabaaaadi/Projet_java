package interfaces;

import model.*;
import exceptions.*;

public interface Reservable {
    boolean estDisponible(Match match, ZonePlace zone) throws BilletIndisponibleException;
    Billet reserverBillet(Client client, Match match, ZonePlace zone) 
        throws BilletIndisponibleException, DonneeInvalideException, AccreditationRefuseeException;
    boolean annulerBillet(String codeBillet) throws DonneeInvalideException;
}