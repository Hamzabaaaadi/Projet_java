
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

    // Exporter tous les billets réservés dans un fichier CSV
    public void exporterBilletsCSV(String cheminFichier) {
        try {
            boolean fileExists = new java.io.File(cheminFichier).exists();
            java.io.FileWriter writer = new java.io.FileWriter(cheminFichier, true); // append mode
            if (!fileExists) {
                writer.write("CodeBillet,NomClient,EmailClient,CodeMatch,Equipe1,Equipe2,Zone,Statut,Montant,DateReservation\n");
            }
            Billet billet = billets.get(billets.size() - 1); // dernier billet réservé
            String ligne = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s\n",
                billet.getCodeBillet(),
                billet.getClient().getNom(),
                billet.getClient().getEmail(),
                billet.getMatch().getCodeMatch(),
                billet.getMatch().getEquipe1(),
                billet.getMatch().getEquipe2(),
                billet.getZone().getNomZone(),
                billet.getStatut(),
                billet.getMontant(),
                billet.getDateReservation()
            );
            writer.write(ligne);
            writer.close();
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de l'export CSV: " + e.getMessage());
        }
    }
    

    // Exporter tous les billets réservés dans un fichier JSON
    public void exporterBilletsJSON(String cheminFichier) {
        try {
            // Utiliser un Set pour éviter les doublons
            Set<String> codesBillets = new HashSet<>();
            List<String> billetsJson = new ArrayList<>();
            for (Billet billet : billets) {
                if (!codesBillets.contains(billet.getCodeBillet())) {
                    codesBillets.add(billet.getCodeBillet());
                    String json =
                        "  {\n" +
                        "    \"codeBillet\": \"" + billet.getCodeBillet() + "\",\n" +
                        "    \"nomClient\": \"" + billet.getClient().getNom() + "\",\n" +
                        "    \"emailClient\": \"" + billet.getClient().getEmail() + "\",\n" +
                        "    \"codeMatch\": \"" + billet.getMatch().getCodeMatch() + "\",\n" +
                        "    \"equipe1\": \"" + billet.getMatch().getEquipe1() + "\",\n" +
                        "    \"equipe2\": \"" + billet.getMatch().getEquipe2() + "\",\n" +
                        "    \"zone\": \"" + billet.getZone().getNomZone() + "\",\n" +
                        "    \"statut\": \"" + billet.getStatut() + "\",\n" +
                        "    \"montant\": " + String.format(java.util.Locale.US, "%.2f", billet.getMontant()) + ",\n" +
                        "    \"dateReservation\": \"" + billet.getDateReservation() + "\"\n" +
                        "  }";
                    billetsJson.add(json);
                }
            }
            java.io.FileWriter writer = new java.io.FileWriter(cheminFichier);
            writer.write("[\n");
            for (int i = 0; i < billetsJson.size(); i++) {
                writer.write(billetsJson.get(i));
                if (i < billetsJson.size() - 1) {
                    writer.write(",\n");
                } else {
                    writer.write("\n");
                }
            }
            writer.write("]\n");
            writer.close();
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de l'export JSON: " + e.getMessage());
        }
    }

    // Charger les billets existants depuis un fichier JSON (au démarrage)
    public void chargerBilletsDepuisJSON(String cheminFichier) {
        java.io.File file = new java.io.File(cheminFichier);
        if (!file.exists()) return;

        try {
            StringBuilder sb = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(file);
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append('\n');
            }
            scanner.close();
            String contenu = sb.toString().trim();
            if (!contenu.startsWith("[") || !contenu.endsWith("]")) return;
            // Extraire objets JSON simples
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{[^}]*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(contenu);
            Set<String> codesExistants = new HashSet<>();
            for (Billet b : billets) codesExistants.add(b.getCodeBillet());

            while (m.find()) {
                String obj = m.group();
                // Récupérer les champs via recherche simple
                String code = extractJsonString(obj, "codeBillet");
                if (code == null || codesExistants.contains(code)) continue; // éviter doublons

                String email = extractJsonString(obj, "emailClient");
                String nomClient = extractJsonString(obj, "nomClient");
                String codeMatch = extractJsonString(obj, "codeMatch");
                String zoneNom = extractJsonString(obj, "zone");
                String statut = extractJsonString(obj, "statut");
                String montantStr = extractJsonString(obj, "montant");
                String dateStr = extractJsonString(obj, "dateReservation");

                double montant = 0.0;
                try { montant = Double.parseDouble(montantStr); } catch (Exception ex) { }

                java.time.LocalDateTime dateRes = null;
                try { dateRes = java.time.LocalDateTime.parse(dateStr); } catch (Exception ex) { dateRes = java.time.LocalDateTime.now(); }

                // Trouver ou créer client
                Client client = null;
                if (email != null) {
                    for (Client c : clients) {
                        if (c.getEmail().equalsIgnoreCase(email)) { client = c; break; }
                    }
                }
                if (client == null) {
                    try {
                        client = new model.Spectateur(nomClient == null ? "Inconnu" : nomClient, email == null ? "inconnu@local" : email, false);
                        clients.add(client);
                    } catch (Exception ex) {
                        // Si échec création client, ignorer ce billet
                        continue;
                    }
                }

                // Trouver le match
                Match match = null;
                if (codeMatch != null) {
                    for (Match mm : matchs) {
                        if (mm.getCodeMatch().equals(codeMatch)) { match = mm; break; }
                    }
                }
                if (match == null) {
                    // Impossible de recréer le match sans données complètes -> ignorer
                    continue;
                }

                // Trouver la zone
                ZonePlace zone = null;
                List<ZonePlace> zones = zonesParMatch.get(match);
                if (zones != null) {
                    for (ZonePlace z : zones) {
                        if (z.getNomZone().equals(zoneNom)) { zone = z; break; }
                    }
                }
                if (zone == null) {
                    // Ignorer si zone non trouvée
                    continue;
                }

                // Créer le Billet en préservant le code et la date
                Billet billet = new Billet(code, client, match, zone, montant, statut, dateRes);
                billets.add(billet);
                // Mettre à jour le quota vendu pour la zone
                zone.incrementerVentes();
                codesExistants.add(code);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement JSON: " + e.getMessage());
        }
    }

    private String extractJsonString(String obj, String key) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\s*:\s*(?:\"([^\"]*)\"|([0-9]+(?:\\.[0-9]+)?))");
            java.util.regex.Matcher m = p.matcher(obj);
            if (m.find()) {
                if (m.group(1) != null) return m.group(1);
                if (m.group(2) != null) return m.group(2);
            }
        } catch (Exception e) { }
        return null;
    }

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

        // Sauvegarder tous les billets dans CSV et JSON après chaque réservation
        exporterBilletsCSV("exports/billets_reserves.csv");
        exporterBilletsJSON("exports/billets_reserves.json");

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
    
    public void ajouterZonePourMatch(Match match, ZonePlace zone) throws DonneeInvalideException {
        if (!zonesParMatch.containsKey(match)) {
            zonesParMatch.put(match, new ArrayList<>());
        }
        // Vérifier que la capacité totale des zones ne dépasse pas la capacité du stade pour ce match
        List<ZonePlace> existing = zonesParMatch.get(match);
        int totalCapacite = zone.getCapacite();
        for (ZonePlace z : existing) {
            totalCapacite += z.getCapacite();
        }
        int capaciteMatch = match.getCapacite();
        if (totalCapacite > capaciteMatch) {
            throw new DonneeInvalideException("Capacité totale des zones (" + totalCapacite + ") dépasse la capacité du match (" + capaciteMatch + ") pour le match " + match.getCodeMatch());
        }
        zonesParMatch.get(match).add(zone);
    }

    // Retourne la capacité restante disponible pour un match (capacité du match - somme des zones)
    public int getCapaciteRestante(Match match) {
        if (!zonesParMatch.containsKey(match)) return match.getCapacite();
        int total = 0;
        for (ZonePlace z : zonesParMatch.get(match)) total += z.getCapacite();
        return Math.max(0, match.getCapacite() - total);
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

    // Calculer le CA total en prenant tous les billets non annulés (réservés ou confirmés)
    public double calculerCATotalReservations() {
        double ca = 0.0;
        for (Billet b : billets) {
            if (!"ANNULE".equalsIgnoreCase(b.getStatut())) {
                ca += b.getMontant();
            }
        }
        return ca;
    }
    
    public void genererRapportVentes() {
        System.out.println("\n=== RAPPORT DES VENTES ===");
        System.out.printf("Chiffre d'affaires (payé): %.2f DH%n", calculerCATotal());
        System.out.printf("Chiffre d'affaires (tous billets non annulés): %.2f DH%n", calculerCATotalReservations());
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

    // Exporter le même rapport dans un fichier texte (persistance)
    public void exporterRapportTXT(String cheminFichier) {
        try (java.io.FileWriter writer = new java.io.FileWriter(cheminFichier)) {
                writer.write("=== RAPPORT DES VENTES ===\n");
            writer.write(String.format("Chiffre d'affaires (payé): %.2f DH\n", calculerCATotal()));
            writer.write(String.format("Chiffre d'affaires (tous billets non annulés): %.2f DH\n", calculerCATotalReservations()));
            writer.write(String.format("Nombre total de billets vendus: %d\n", billets.size()));
            writer.write(String.format("Nombre de clients: %d\n\n", clients.size()));

            // Top 5 billets
            List<Billet> billetsTries = new ArrayList<>(billets);
            billetsTries.sort((b1, b2) -> Double.compare(b2.getMontant(), b1.getMontant()));
            writer.write("Top 5 billets les plus chers:\n");
            for (int i = 0; i < Math.min(5, billetsTries.size()); i++) {
                Billet b = billetsTries.get(i);
                writer.write(String.format("  %d. %s - %.2f DH\n", i+1, b.getCodeBillet(), b.getMontant()));
            }

            // Ne pas inclure les détails des billets dans le rapport texte (demande de l'utilisateur)

        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de l'export du rapport TXT: " + e.getMessage());
        }
    }
    
    // Getters pour accéder aux collections
    public List<Match> getMatchs() { return matchs; }
    public Map<Match, List<ZonePlace>> getZonesParMatch() { return zonesParMatch; }
    public List<Client> getClients() { return clients; }
    public List<Billet> getBillets() { return billets; }
}