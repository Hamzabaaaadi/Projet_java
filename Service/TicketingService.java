package service;

import interfaces.*;
import model.*;
import exceptions.*;
import java.util.*;
import java.time.LocalDateTime;

public class TicketingService implements Reservable, Payable {
    // Collections pour le stockage
    private List<Match> matchs = new ArrayList<>();
    private Map<Match, List<ZonePlace>> zonesParMatch = new HashMap<>();
    private List<Client> clients = new ArrayList<>(); // Polymorphisme: Client peut être Spectateur ou Media
    private List<Billet> billets = new ArrayList<>();
    private List<Paiement> paiements = new ArrayList<>();
    
    private final double PRIX_BASE = 50.0; // Prix de base pour calcul
    
    // ==== IMPLÉMENTATION DE L'INTERFACE RESERVABLE ====
    
    @Override
    public boolean estDisponible(Match match, ZonePlace zone) throws BilletIndisponibleException {
        // Vérifier si la zone existe pour ce match
        if (!zonesParMatch.containsKey(match)) {
            throw new BilletIndisponibleException("Match non trouvé");
        }
        
        // Vérifier si la zone appartient à ce match
        List<ZonePlace> zones = zonesParMatch.get(match);
        if (!zones.contains(zone)) {
            throw new BilletIndisponibleException("Zone non disponible pour ce match");
        }
        
        // Vérifier le quota
        if (!zone.estDisponible()) {
            throw new BilletIndisponibleException("Quota épuisé pour cette zone: " + zone.getNomZone());
        }
        
        return true;
    }
    
    @Override
        public Billet reserverBillet(Client client, Match match, ZonePlace zone) 
            throws BilletIndisponibleException, DonneeInvalideException, AccreditationRefuseeException {
        
        // Vérifier disponibilité
        estDisponible(match, zone);
        
        // Vérifier si c'est un média non accrédité
        if (client instanceof Media) {
            Media media = (Media) client;
            if (!media.isAccreditationValidee()) {
                throw new AccreditationRefuseeException(
                    "Média non accrédité: " + media.getNom());
            }
        }
        
        // Calculer le prix selon les règles métier
        double prix = calculerPrix(match, zone, client);
        
        // Créer le billet
        Billet billet = new Billet(client, match, zone, prix);
        
        // Mettre à jour le quota
        zone.incrementerVentes();
        
        // Ajouter à la collection
        billets.add(billet);
        
        return billet;
    }
    
    @Override
    public boolean annulerBillet(String codeBillet) throws DonneeInvalideException {
        // Chercher le billet
        Billet billet = chercherBilletParCode(codeBillet);
        if (billet == null) {
            throw new DonneeInvalideException("Billet non trouvé: " + codeBillet);
        }
        
        // Vérifier s'il n'est pas déjà annulé
        if ("ANNULE".equals(billet.getStatut())) {
            throw new DonneeInvalideException("Billet déjà annulé");
        }
        
        // Calculer la pénalité si annulation < 24h
        LocalDateTime maintenant = LocalDateTime.now();
        long heuresAvantMatch = java.time.Duration.between(maintenant, 
            billet.getMatch().getDateHeure()).toHours();
        
        double montantRemboursement = billet.getMontant();
        if (heuresAvantMatch < 24) {
            // Appliquer pénalité de 20%
            double penalite = billet.getMontant() * 0.20;
            montantRemboursement -= penalite;
            System.out.printf("Pénalité de 20%% appliquée: %.2f DH déduits%n", penalite);
        }
        
        // Annuler le billet
        billet.setStatut("ANNULE");
        billet.setMontant(montantRemboursement);
        
        // Libérer la place
        billet.getZone().decrementerVentes();
        
        System.out.printf("Billet %s annulé. Remboursement: %.2f DH%n", 
            codeBillet, montantRemboursement);
        
        return true;
    }
    
    // ==== IMPLÉMENTATION DE L'INTERFACE PAYABLE ====
    
    @Override
    public Paiement payer(Billet billet, String modePaiement) throws PaiementInvalideException {
        // Vérifier le mode de paiement
        if (!Arrays.asList("CARTE", "ESPECES", "VIREMENT").contains(modePaiement)) {
            throw new PaiementInvalideException("Mode de paiement invalide: " + modePaiement);
        }
        
        // Vérifier si le billet est déjà payé
        if ("CONFIRME".equals(billet.getStatut())) {
            throw new PaiementInvalideException("Billet déjà payé");
        }
        
        // Créer le paiement
        Paiement paiement = new Paiement(billet, modePaiement);
        
        // Simuler le traitement du paiement (dans la réalité, appel à une API bancaire)
        if (Math.random() > 0.1) { // 90% de succès simulé
            paiement.setStatut("PAYE");
            billet.setStatut("CONFIRME");
            System.out.println("Paiement réussi pour le billet: " + billet.getCodeBillet());
        } else {
            paiement.setStatut("REFUSE");
            throw new PaiementInvalideException("Paiement refusé par la banque");
        }
        
        // Enregistrer le paiement
        paiements.add(paiement);
        
        return paiement;
    }
    
    @Override
    public boolean verifierPaiement(Billet billet) {
        for (Paiement p : paiements) {
            if (p.getBillet().equals(billet) && "PAYE".equals(p.getStatut())) {
                return true;
            }
        }
        return false;
    }
    
    // ==== MÉTHODES MÉTIER ====
    
    private double calculerPrix(Match match, ZonePlace zone, Client client) {
        double base = zone.getPrix();
        if (base <= 0) base = PRIX_BASE * zone.getCoefficientPrix();
        double prix = base * match.getCoefficientImportance();
        double reduction = client.getReduction();
        return prix * (1 - reduction);
    }
    
    public void ajouterMatch(Match match) throws DonneeInvalideException {
        // Vérifier doublon (mêmes équipes, ordre non-significatif)
        for (Match m : matchs) {
            if ((m.getEquipe1().equalsIgnoreCase(match.getEquipe1()) && m.getEquipe2().equalsIgnoreCase(match.getEquipe2())) ||
                (m.getEquipe1().equalsIgnoreCase(match.getEquipe2()) && m.getEquipe2().equalsIgnoreCase(match.getEquipe1()))) {
                // doublon trouvé
                throw new DonneeInvalideException("Match déjà existant entre ces équipes: " + match.getEquipe1() + " vs " + match.getEquipe2());
            }
        }
        matchs.add(match);
        zonesParMatch.put(match, new ArrayList<>());
    }
    
    public void ajouterZonePourMatch(Match match, ZonePlace zone) {
        if (!zonesParMatch.containsKey(match)) {
            zonesParMatch.put(match, new ArrayList<>());
        }
        zonesParMatch.get(match).add(zone);
    }
    
    public void ajouterClient(Client client) {
        clients.add(client);
    }
    
    public boolean accrediterMedia(String emailMedia) throws AccreditationRefuseeException {
        for (Client client : clients) {
            if (client instanceof Media && client.getEmail().equals(emailMedia)) {
                Media media = (Media) client;
                
                // Vérifier les documents (simulation)
                if (!verifierDocumentsMedia(media)) {
                    throw new AccreditationRefuseeException(
                        "Documents insuffisants pour le média: " + media.getNom());
                }
                
                media.setAccreditationValidee(true);
                System.out.println("Média accrédité: " + media.getNom());
                return true;
            }
        }
        throw new AccreditationRefuseeException("Média non trouvé: " + emailMedia);
    }
    
    private boolean verifierDocumentsMedia(Media media) {
        // Simulation: 80% de chance d'avoir les bons documents
        return Math.random() > 0.2;
    }
    
    // ==== MÉTHODES DE RECHERCHE ET RAPPORTS ====
    
    public Billet chercherBilletParCode(String code) {
        for (Billet b : billets) {
            if (b.getCodeBillet().equals(code)) {
                return b;
            }
        }
        return null;
    }
    
    public List<Billet> getBilletsParClient(Client client) {
        List<Billet> result = new ArrayList<>();
        for (Billet b : billets) {
            if (b.getClient().equals(client)) {
                result.add(b);
            }
        }
        return result;
    }
    
    public List<Billet> getBilletsParMatch(Match match) {
        List<Billet> result = new ArrayList<>();
        for (Billet b : billets) {
            if (b.getMatch().equals(match)) {
                result.add(b);
            }
        }
        return result;
    }
    
    public double calculerCATotal() {
        double caTotal = 0;
        for (Paiement p : paiements) {
            if ("PAYE".equals(p.getStatut())) {
                caTotal += p.getBillet().getMontant();
            }
        }
        return caTotal;
    }
    
    public void genererRapportVentes() {
        System.out.println("\n=== RAPPORT DES VENTES ===");
        System.out.printf("Chiffre d'affaires total: %.2f DH%n", calculerCATotal());
        System.out.printf("Nombre total de billets vendus: %d%n", billets.size());
        System.out.printf("Nombre de clients: %d%n", clients.size());
        
        // Trier les ventes par montant (descendant)
        List<Billet> billetsTries = new ArrayList<>(billets);
        billetsTries.sort((b1, b2) -> Double.compare(b2.getMontant(), b1.getMontant()));
        
        System.out.println("\nTop 5 billets les plus chers:");
        for (int i = 0; i < Math.min(5, billetsTries.size()); i++) {
            Billet b = billetsTries.get(i);
            System.out.printf("  %d. %s - %.2f DH%n", i+1, b.getCodeBillet(), b.getMontant());
        }
    }
    
    // Getters pour accéder aux collections
    public List<Match> getMatchs() { return matchs; }
    public Map<Match, List<ZonePlace>> getZonesParMatch() { return zonesParMatch; }
    public List<Client> getClients() { return clients; }
    public List<Billet> getBillets() { return billets; }
}