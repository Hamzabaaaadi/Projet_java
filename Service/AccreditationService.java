package service;

import model.Media;
import exceptions.AccreditationRefuseeException;
import java.util.*;

public class AccreditationService {
    private List<Media> mediasAccredites = new ArrayList<>();
    private Queue<Media> demandes = new LinkedList<>();

    public void soumettreDemande(Media media) {
        demandes.add(media);
    }

    public void afficherDemandesEnAttente() {
        System.out.println("Demandes en attente:");
        if (demandes.isEmpty()) {
            System.out.println("  (aucune)");
            return;
        }
        for (Media m : demandes) {
            System.out.println("  - " + m.getEmail() + " (" + m.getTypeMedia() + ")");
        }
    }

    public boolean traiterDemande(String email, boolean approuver, String commentaire) throws AccreditationRefuseeException {
        for (Iterator<Media> it = demandes.iterator(); it.hasNext();) {
            Media m = it.next();
            if (m.getEmail().equalsIgnoreCase(email)) {
                it.remove();
                if (!approuver) {
                    throw new AccreditationRefuseeException("Demande refusée: " + commentaire);
                }
                m.setAccreditationValidee(true);
                mediasAccredites.add(m);
                System.out.println("Média accrédité: " + m.getEmail());
                return true;
            }
        }
        throw new AccreditationRefuseeException("Demande non trouvée pour: " + email);
    }

    public void afficherMediasAccredites() {
        System.out.println("Médias accrédités:");
        if (mediasAccredites.isEmpty()) {
            System.out.println("  (aucun)");
            return;
        }
        for (Media m : mediasAccredites) {
            System.out.println("  - " + m.getEmail() + " (" + m.getTypeMedia() + ")");
        }
    }

    public void revoquerAccreditation(String email, String raison) {
        mediasAccredites.removeIf(m -> m.getEmail().equalsIgnoreCase(email));
        System.out.println("Accréditation révoquée pour: " + email + " (" + raison + ")");
    }
}