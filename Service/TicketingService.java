
package service;

import interfaces.*;
import model.*;
import exceptions.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        // Réécrit entièrement le fichier CSV à partir de la liste `billets`
        try (java.io.FileWriter writer = new java.io.FileWriter(cheminFichier)) {
            writer.write("CodeBillet,NomClient,EmailClient,CodeMatch,Equipe1,Equipe2,Zone,Statut,Montant,DateReservation,MediaAccredite\n");
            DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (Billet billet : billets) {
                String mediaAccredite = "";
                if (billet.getClient() instanceof model.Media) {
                    model.Media m = (model.Media) billet.getClient();
                    mediaAccredite = m.isAccreditationValidee() ? "OUI" : "NON";
                }
                String dateStr = billet.getDateReservation() != null ? billet.getDateReservation().format(df) : "";
                String ligne = String.format(Locale.ROOT, "%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s,%s\n",
                    billet.getCodeBillet(),
                    billet.getClient().getNom(),
                    billet.getClient().getEmail(),
                    billet.getMatch().getCodeMatch(),
                    billet.getMatch().getEquipe1(),
                    billet.getMatch().getEquipe2(),
                    billet.getZone().getNomZone(),
                    billet.getStatut(),
                    billet.getMontant(),
                    dateStr,
                    mediaAccredite
                );
                writer.write(ligne);
            }
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

        // Si client est un média non accrédité, il paie 50% (déjà géré par getReduction), mais on autorise la réservation
        // (On nève plus d'exception ici)

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
        
        // Conserver le montant original pour le calcul du CA
        double montantOriginal = billet.getMontant();
        double penalite = 0.0;
        if (heuresAvantMatch < 24) {
            // Appliquer pénalité de 20% (retenue par l'organisation)
            penalite = montantOriginal * 0.20;
            System.out.printf("Pénalité de 20%% appliquée: %.2f DH déduits%n", penalite);
        }
        double montantRemboursement = montantOriginal - penalite;

        // Annuler le billet (laisser le montant original pour l'historique des paiements)
        billet.setStatut("ANNULE");

        // Libérer la place
        billet.getZone().decrementerVentes();

        // Supprimer le billet des exports et de la liste principale
        // (on garde éventuellement les objets Paiement pour l'historique CA)
        billets.remove(billet);
        // Ré-écrire les fichiers d'export complets
        exporterBilletsCSV("exports/billets_reserves.csv");
        exporterBilletsJSON("exports/billets_reserves.json");

        System.out.printf("Billet %s annulé. Remboursement: %.2f DH%n", codeBillet, montantRemboursement);

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
            System.out.println("Ce billet a déjà été payé. Paiement supplémentaire ignoré.");
            throw new PaiementInvalideException("Billet déjà payé");
        }

        // Créer le paiement
        Paiement paiement = new Paiement(billet, modePaiement);

        // Simuler le traitement du paiement (dans la réalité, appel à une API bancaire)
        double tauxSucces = 0.98; // 98% de succès simulé
        if (Math.random() < tauxSucces) {
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
        double coefImportance = match.getCoefficientImportance();
        // Si importance élevée (> 1.5), augmenter le prix de la zone de 20%
        if (coefImportance > 1.5) {
            base = base * 1.2;
        }
        double prix = base * coefImportance;
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
        Set<String> billetsPayes = new HashSet<>();
        for (Paiement p : paiements) {
            if ("PAYE".equals(p.getStatut()) &&
                ("CARTE".equalsIgnoreCase(p.getModePaiement()) ||
                 "ESPECES".equalsIgnoreCase(p.getModePaiement()) ||
                 "VIREMENT".equalsIgnoreCase(p.getModePaiement()))) {
                Billet b = p.getBillet();
                // Ne compter qu'une seule fois chaque billet payé
                if (!billetsPayes.contains(b.getCodeBillet())) {
                    if ("ANNULE".equalsIgnoreCase(b.getStatut())) {
                        // Si billet annulé, ne compter que la pénalité (20% du montant initial)
                        double penalite = b.getMontant() * 0.20;
                        caTotal += penalite;
                    } else {
                        caTotal += b.getMontant();
                    }
                    billetsPayes.add(b.getCodeBillet());
                }
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

        System.out.println("\nListe complète des billets vendus:");
        for (Billet b : billetsTries) {
            System.out.printf("- %s | Client: %s | Match: %s | Zone: %s | Statut: %s | Montant: %.2f DH | Date: %s%n",
                b.getCodeBillet(), b.getClient().getNom(), b.getMatch().getCodeMatch(), b.getZone().getNomZone(), b.getStatut(), b.getMontant(), b.getDateReservation());
        }
    }

    // Exporter le même rapport dans un fichier texte (persistance)
    public void exporterRapportTXT(String cheminFichier) {
        try (java.io.FileWriter writer = new java.io.FileWriter(cheminFichier)) {
                writer.write("=== RAPPORT DES VENTES ===\n");
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
    
    // Sauvegarder les clients dans un fichier JSON simple
    public void sauvegarderClients(String cheminFichier) {
        try (java.io.FileWriter writer = new java.io.FileWriter(cheminFichier)) {
            writer.write("[\n");
            for (int i = 0; i < clients.size(); i++) {
                Client c = clients.get(i);
                String type = (c instanceof Media) ? "Media" : "Spectateur";
                String extra = "";
                if (c instanceof Media) {
                    Media m = (Media) c;
                    extra = String.format(",\n    \"typeMedia\": \"%s\",\n    \"accredite\": %s", m.getTypeMedia(), m.isAccreditationValidee() ? "true" : "false");
                } else if (c instanceof Spectateur) {
                    Spectateur s = (Spectateur) c;
                    extra = String.format(",\n    \"fidele\": %s", s.isFidele() ? "true" : "false");
                }
                String obj = String.format(java.util.Locale.ROOT,
                    "  {\n    \"type\": \"%s\",\n    \"nom\": \"%s\",\n    \"email\": \"%s\"%s\n  }",
                    type, c.getNom(), c.getEmail(), extra);
                writer.write(obj);
                if (i < clients.size() - 1) writer.write(",\n"); else writer.write("\n");
            }
            writer.write("]\n");
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la sauvegarde des clients: " + e.getMessage());
        }
    }

    // Charger les clients depuis un fichier JSON simple
    public void chargerClientsDepuisJSON(String cheminFichier) {
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
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{[^}]*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(contenu);
            while (m.find()) {
                String obj = m.group();
                String type = extractJsonString(obj, "type");
                String nom = extractJsonString(obj, "nom");
                String email = extractJsonString(obj, "email");
                if (type == null || nom == null || email == null) continue;
                boolean exists = false;
                for (Client c : clients) {
                    if (c.getEmail().equalsIgnoreCase(email)) { exists = true; break; }
                }
                if (exists) continue;
                if (type.equals("Media")) {
                    String typeMedia = extractJsonString(obj, "typeMedia");
                    boolean accredite = "true".equalsIgnoreCase(extractJsonString(obj, "accredite"));
                    Media media = new Media(nom, email, typeMedia == null ? "Presse" : typeMedia);
                    if (accredite) media.setAccreditationValidee(true);
                    clients.add(media);
                } else {
                    boolean fidele = "true".equalsIgnoreCase(extractJsonString(obj, "fidele"));
                    Spectateur spect = new Spectateur(nom, email, fidele);
                    clients.add(spect);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des clients: " + e.getMessage());
        }
    }

    // Getters pour accéder aux collections
    public List<Match> getMatchs() { return matchs; }
    public Map<Match, List<ZonePlace>> getZonesParMatch() { return zonesParMatch; }
    public List<Client> getClients() { return clients; }
    public List<Billet> getBillets() { return billets; }

    // Sauvegarder les matchs dans un fichier JSON simple
    public void sauvegarderMatchs(String cheminFichier) {
        try (java.io.FileWriter writer = new java.io.FileWriter(cheminFichier)) {
            writer.write("[\n");
            for (int i = 0; i < matchs.size(); i++) {
                Match mm = matchs.get(i);
                String obj = String.format(Locale.ROOT,
                    "  {\n    \"equipe1\": \"%s\",\n    \"equipe2\": \"%s\",\n    \"stade\": \"%s\",\n    \"dateHeure\": \"%s\",\n    \"importance\": %.2f,\n    \"capacite\": %d\n  }",
                    mm.getEquipe1(), mm.getEquipe2(), mm.getStade().toString(), mm.getDateHeure().toString(), mm.getImportance(), mm.getCapacite());
                writer.write(obj);
                if (i < matchs.size() - 1) writer.write(",\n"); else writer.write("\n");
            }
            writer.write("]\n");
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la sauvegarde des matchs: " + e.getMessage());
        }
    }

    // Charger les matchs depuis un fichier JSON simple
    public void chargerMatchsDepuisJSON(String cheminFichier) {
        java.io.File file = new java.io.File(cheminFichier);
        if (!file.exists()) return;
        try {
            StringBuilder sb = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(file);
            while (scanner.hasNextLine()) sb.append(scanner.nextLine()).append('\n');
            scanner.close();
            String contenu = sb.toString();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{[^}]*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(contenu);
            while (m.find()) {
                String obj = m.group();
                String e1 = extractJsonString(obj, "equipe1");
                String e2 = extractJsonString(obj, "equipe2");
                String stadeStr = extractJsonString(obj, "stade");
                String dateStr = extractJsonString(obj, "dateHeure");
                String impStr = extractJsonString(obj, "importance");
                String capStr = extractJsonString(obj, "capacite");
                if (e1 == null || e2 == null || stadeStr == null || dateStr == null) continue;
                try {
                    model.Stade stade = model.Stade.fromString(stadeStr);
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(dateStr);
                    double importance = 1.0;
                    try { importance = Double.parseDouble(impStr); } catch (Exception ex) { }
                    Match match = new Match(e1, e2, stade, dt, importance);
                    if (capStr != null) {
                        try { match.setCapacite(Integer.parseInt(capStr)); } catch (Exception ex) { }
                    }
                    // Ajouter si non présent
                    try { ajouterMatch(match); } catch (DonneeInvalideException ex) { /* ignore duplicates */ }
                } catch (Exception ex) {
                    // ignorer si parse ou création échoue
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des matchs: " + e.getMessage());
        }
    }
}