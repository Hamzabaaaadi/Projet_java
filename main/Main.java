package main;

import model.*;
import service.*;
import exceptions.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;

public class Main {
    private static TicketingService service = new TicketingService();
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.println("=== SYSTÈME DE GESTION DE BILLETTERIE CAN 2025 ===");
        
        // Initialisation avec des données de test
        initialiserDonneesTest();
        // Charger les billets précédemment sauvegardés (persistés)
        service.chargerBilletsDepuisJSON("exports/billets_reserves.json");
        
        boolean continuer = true;
        while (continuer) {
            afficherMenu();
            int choix = lireChoix();
            
            try {
                switch (choix) {
                    case 1 -> ajouterMatch();
                    case 2 -> ajouterZone();
                    case 3 -> ajouterClient();
                    case 4 -> accrediterMedia();
                    case 5 -> vendreBillet();
                    case 6 -> annulerBillet();
                    case 7 -> afficherBilletsClient();
                    case 8 -> genererRapports();
                    case 9 -> {
                        continuer = false;
                        System.out.println("Au revoir!");
                    }
                    default -> System.out.println("Choix invalide!");
                }
            } catch (Exception e) {
                System.out.println("ERREUR: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        scanner.close();
    }
    
    private static void afficherMenu() {
        System.out.println("\n=== MENU PRINCIPAL ===");
        System.out.println("1. Ajouter un match");
        System.out.println("2. Ajouter une zone de places");
        System.out.println("3. Ajouter un client (Spectateur/Média)");
        System.out.println("4. Accréditer un média");
        System.out.println("5. Vendre un billet");
        System.out.println("6. Annuler un billet");
        System.out.println("7. Afficher les billets d'un client");
        System.out.println("8. Générer des rapports");
        System.out.println("9. Quitter");
        System.out.print("Votre choix: ");
    }
    
    private static int lireChoix() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // ==== IMPLÉMENTATION DES OPTIONS DU MENU ====
    
    private static void ajouterMatch() throws DonneeInvalideException {
        System.out.println("\n--- Ajout d'un match ---");
        
        System.out.print("Équipe 1: ");
        String equipe1 = scanner.nextLine();
        
        System.out.print("Équipe 2: ");
        String equipe2 = scanner.nextLine();
        
        // Choix du stade (enum)
        System.out.println("Stades disponibles:");
        model.Stade[] stades = model.Stade.values();
        for (int i = 0; i < stades.length; i++) {
            System.out.println((i+1) + ". " + stades[i].toString());
        }
        System.out.print("Choisissez un stade (numéro ou nom): ");
        String stadeInput = scanner.nextLine().trim();
        model.Stade stadeObj = null;
        try {
            int idx = Integer.parseInt(stadeInput) - 1;
            if (idx >= 0 && idx < stades.length) stadeObj = stades[idx];
        } catch (NumberFormatException ignored) { }
        if (stadeObj == null) {
            stadeObj = model.Stade.fromString(stadeInput);
        }
        if (stadeObj == null) throw new DonneeInvalideException("Stade inconnu: " + stadeInput);
        
        System.out.print("Date (AAAA-MM-JJ): ");
        String dateStr = scanner.nextLine().trim();

        System.out.print("Heure (HH:MM ou HH): ");
        String heureStr = scanner.nextLine().trim();

        // Normaliser date et heure (accepte 1 ou 2 chiffres pour mois/jour/heure)
        try {
            String[] dateParts = dateStr.split("-");
            if (dateParts.length != 3) throw new IllegalArgumentException("Format date invalide");
            String year = dateParts[0];
            String month = pad2(dateParts[1]);
            String day = pad2(dateParts[2]);
            String normalizedDate = year + "-" + month + "-" + day;

            String hourPart = "00";
            String minutePart = "00";
            if (heureStr.contains(":")) {
                String[] hm = heureStr.split(":");
                hourPart = pad2(hm[0]);
                minutePart = pad2(hm.length > 1 ? hm[1] : "0");
            } else if (!heureStr.isEmpty()) {
                hourPart = pad2(heureStr);
            }

            LocalDateTime dateHeure = LocalDateTime.parse(normalizedDate + "T" + hourPart + ":" + minutePart + ":00");
            
            System.out.print("Importance (1=Normal, 2=Important, 3=Très important): ");
            int importance = Integer.parseInt(scanner.nextLine());

            Match match = new Match(equipe1, equipe2, stadeObj, dateHeure, importance);
            service.ajouterMatch(match);

            System.out.println("Match ajouté avec succès: " + match.getCodeMatch());
            return;
        } catch (Exception e) {
            throw new DonneeInvalideException("Date/heure invalide: " + e.getMessage());
        }
    }

    private static String pad2(String s) {
        if (s == null) return "00";
        s = s.trim();
        if (s.length() == 0) return "00";
        if (s.length() == 1) return "0" + s;
        return s;
    }
    
    private static void ajouterZone() throws DonneeInvalideException {
        System.out.println("\n--- Ajout d'une zone ---");
        
        // Afficher les matchs disponibles
        List<Match> matchs = service.getMatchs();
        if (matchs.isEmpty()) {
            System.out.println("Aucun match disponible. Ajoutez d'abord un match.");
            return;
        }
        
        System.out.println("Matchs disponibles:");
        for (int i = 0; i < matchs.size(); i++) {
            System.out.println((i+1) + ". " + matchs.get(i));
        }
        
        System.out.print("Choisissez un match (numéro): ");
        int choixMatch = Integer.parseInt(scanner.nextLine()) - 1;
        
        if (choixMatch < 0 || choixMatch >= matchs.size()) {
            System.out.println("Choix invalide!");
            return;
        }
        
        Match match = matchs.get(choixMatch);
        // Afficher la capacité actuelle du match et la capacité restante
        System.out.println("Capacité actuelle du match: " + match.getCapacite());
        int restante = service.getCapaciteRestante(match);
        System.out.println("Capacité restante disponible pour ce match (avant ajout): " + restante);
        System.out.print("Souhaitez-vous modifier la capacité du match? (oui/non) : ");
        String modCap = scanner.nextLine().trim();
        if (modCap.equalsIgnoreCase("oui") || modCap.equalsIgnoreCase("o")) {
            System.out.print("Nouvelle capacité pour le match: ");
            try {
                int newCap = Integer.parseInt(scanner.nextLine().trim());
                if (newCap <= 0) {
                    System.out.println("Capacité invalide, opération annulée.");
                    return;
                }
                match.setCapacite(newCap);
                System.out.println("Capacité du match mise à jour: " + match.getCapacite());
            } catch (NumberFormatException e) {
                System.out.println("Entrée invalide, opération annulée.");
                return;
            }
            // recalculer restante
            restante = service.getCapaciteRestante(match);
            System.out.println("Capacité restante disponible pour ce match (après modification): " + restante);
        }
        
        // Choix du type de zone (enum)
        model.ZoneType[] zoneTypes = model.ZoneType.values();
        System.out.println("Types de zone disponibles:");
        for (int i = 0; i < zoneTypes.length; i++) {
            System.out.println((i+1) + ". " + zoneTypes[i].toString());
        }
        System.out.print("Choisissez le type de zone (numéro ou nom): ");
        String zoneInput = scanner.nextLine().trim();
        model.ZoneType chosen = null;
        try {
            int idx = Integer.parseInt(zoneInput) - 1;
            if (idx >= 0 && idx < zoneTypes.length) chosen = zoneTypes[idx];
        } catch (NumberFormatException ignored) { }
        if (chosen == null) chosen = model.ZoneType.fromString(zoneInput);
        if (chosen == null) {
            System.out.println("Type de zone invalide!");
            return;
        }

        System.out.print("Capacité: ");
        int capacite = Integer.parseInt(scanner.nextLine());

            System.out.print("Prix de base pour cette zone (laisser vide pour prix par défaut: " + chosen.getDefaultPrice() + "): ");
            String prixInput = scanner.nextLine().trim();
            double prix;
            if (prixInput.isEmpty()) {
                prix = chosen.getDefaultPrice();
            } else {
                prix = Double.parseDouble(prixInput);
            }

        ZonePlace zone = new ZonePlace(chosen, capacite, prix);
        service.ajouterZonePourMatch(match, zone);
        
        System.out.println("Zone ajoutée avec succès pour le match " + match.getCodeMatch());
    }
    
    private static void ajouterClient() throws DonneeInvalideException {
        System.out.println("\n--- Ajout d'un client ---");
        
        System.out.println("Type de client:");
        System.out.println("1. Spectateur");
        System.out.println("2. Média");
        System.out.print("Votre choix: ");
        
        int type = Integer.parseInt(scanner.nextLine());
        
        System.out.print("Nom: ");
        String nom = scanner.nextLine();
        
        System.out.print("Email: ");
        String email = scanner.nextLine();
        
        Client client;
        
        if (type == 1) {
            System.out.print("Client fidèle? (oui/non): ");
            boolean fidele = scanner.nextLine().equalsIgnoreCase("oui");
            client = new Spectateur(nom, email, fidele);
        } else if (type == 2) {
            System.out.print("Type de média (TV/Radio/Presse/Web): ");
            String typeMedia = scanner.nextLine();
            client = new Media(nom, email, typeMedia);
        } else {
            System.out.println("Type invalide!");
            return;
        }
        
        service.ajouterClient(client);
        System.out.println("Client ajouté avec succès: " + client);
    }
    
    private static void accrediterMedia() {
        System.out.println("\n--- Accréditation d'un média ---");
        
        System.out.print("Email du média: ");
        String email = scanner.nextLine();
        
        try {
            service.accrediterMedia(email);
            System.out.println("Accréditation réussie!");
        } catch (AccreditationRefuseeException e) {
            System.out.println("Accréditation refusée: " + e.getMessage());
        }
    }
    
    private static void vendreBillet() {
        System.out.println("\n--- Vente d'un billet ---");
        
        // Choisir un client
            // Réinitialiser les collections existantes
            service.getMatchs().clear();
            service.getClients().clear();
            service.getZonesParMatch().clear();
            service.getBillets().clear();

            // Liste de matchs fournie par l'utilisateur (8 matchs)
            List<Match> createdMatches = new ArrayList<>();

            // Helper pour créer LocalDateTime à partir de chaînes (AAAA-MM-JJ, HH:MM)
            java.util.function.BiFunction<String, String, LocalDateTime> makeDate = (date, time) -> {
                String[] d = date.split("-");
                String year = d[0];
                String month = pad2(d[1]);
                String day = pad2(d[2]);
                String[] hm = time.split(":");
                String hh = pad2(hm[0]);
                String mm = hm.length > 1 ? pad2(hm[1]) : "00";
                return LocalDateTime.parse(year + "-" + month + "-" + day + "T" + hh + ":" + mm + ":00");
            };

            // On mappe les importances utilisateur -> int (1 =>1, 1.5=>2, 2=>3)
            java.util.function.Function<Double, Integer> mapImportance = (d) -> {
                if (d <= 1.0) return 1;
                if (d <= 1.5) return 2;
                return 3;
            };

            // Définition des matchs demandés
            Object[][] matchsSpec = new Object[][]{
                {"Maroc", "Comores", "2025-12-21", "20:00", model.Stade.RABAT_MOULAY_ABDELLAH, 2.0},
                {"Afrique du Sud", "Angola", "2025-12-22", "18:00", model.Stade.MARRAKECH, 1.0},
                {"Mali", "Tunisie", "2026-01-03", "20:00", model.Stade.CASABLANCA, 1.5},
                {"Maroc", "Tanzanie", "2026-01-04", "17:00", model.Stade.RABAT_MOULAY_ABDELLAH, 1.5},
                {"Égypte", "Côte d\'Ivoire", "2026-01-10", "19:00", model.Stade.AGADIR, 1.5},
                {"Maroc", "Nigeria", "2026-01-14", "20:00", model.Stade.RABAT_MOULAY_ABDELLAH, 2.0},
                {"Maroc", "Sénégal", "2026-01-18", "18:00", model.Stade.RABAT_MOULAY_ABDELLAH, 2.0},
                {"Sénégal", "Égypte", "2026-01-14", "18:00", model.Stade.TANGER, 1.5}
            };

            for (Object[] ms : matchsSpec) {
                String e1 = (String) ms[0];
                String e2 = (String) ms[1];
                String date = (String) ms[2];
                String time = (String) ms[3];
                Stade stade = (Stade) ms[4];
                double impD = (Double) ms[5];
                int imp = mapImportance.apply(impD);
                Match match = new Match(e1, e2, stade, makeDate.apply(date, time), imp);
                service.ajouterMatch(match);
                createdMatches.add(match);

                // Ajouter 3 zones par match: VIP, Zone1, Zone2
                ZonePlace z1 = new ZonePlace(ZoneType.VIP, 50, ZoneType.VIP.getDefaultPrice());
                ZonePlace z2 = new ZonePlace(ZoneType.ZONE1, 120, ZoneType.ZONE1.getDefaultPrice());
                ZonePlace z3 = new ZonePlace(ZoneType.ZONE2, 200, ZoneType.ZONE2.getDefaultPrice());
                service.ajouterZonePourMatch(match, z1);
                service.ajouterZonePourMatch(match, z2);
                service.ajouterZonePourMatch(match, z3);
            }

            // Création de 25 clients avec noms marocains (20 spectateurs + 5 médias)
            String[] nomsSpectateurs = new String[]{
                "Youssef El Amrani","Fatima Zahra Benali","Mohamed Idrissi","Khadija El Hajj",
                "Hassan Ouazzani","Imane El Khatib","Rachid Bekkali","Zineb Chraibi",
                "Omar El Fassi","Najat El Idrissi","Soufiane Bennis","Leila Bourhim",
                "Adil Saidi","Meryem Haddad","Anas El Mansouri","Nour El Khattabi",
                "Karim Azmani","Samira El Omari","Reda Amghar","Sara Amrani"
            };

            for (int i = 0; i < nomsSpectateurs.length; i++) {
                String nom = nomsSpectateurs[i];
                Spectateur s = new Spectateur(nom, nom.toLowerCase().replace(' ', '.') + "@mail.ma", (i % 4 == 0));
                service.ajouterClient(s);
            }

            // 5 médias
            String[] nomsMedias = new String[]{"AlAkhbar TV","Radio Maroc","LeMatin","Medi1Web","Yabiladi Press","Chof TV","2M Radio","Hespress","TelQuel Web","L'Opinion Presse","Radio Mars"};
            String[] typesMedia = new String[]{"TV","Radio","Presse","Web","Presse"};
            for (int i = 0; i < nomsMedias.length; i++) {
                Media m = new Media(nomsMedias[i], "media" + (i+1) + "@press.ma", typesMedia[i]);
                service.ajouterClient(m);
            }

            System.out.println("Données de test initialisées: 8 matchs, 25 clients (>=5 médias) et 3 zones/match");

            // ------------------ VENTES: vendre au moins 40 billets ------------------
            List<String> billetsVendusCodes = new ArrayList<>();
            int ventesCible = 40;
            Random rnd = new Random(12345);
            List<Client> clients = service.getClients();
            int clientIdx = 0;
            while (billetsVendusCodes.size() < ventesCible) {
                for (Match match : createdMatches) {
                    List<ZonePlace> zones = service.getZonesParMatch().get(match);
                    for (ZonePlace zone : zones) {
                        if (billetsVendusCodes.size() >= ventesCible) break;
                        // choisir client cycliquement
                        Client client = clients.get(clientIdx % clients.size());
                        clientIdx++;
                        try {
                            Billet b = service.reserverBillet(client, match, zone);
                            // pour la simulation, alterner paiement valide et parfois refuser volontairement
                            try {
                                // 90% chance d'utiliser un mode valide
                                if (rnd.nextDouble() < 0.9) {
                                    service.payer(b, "CARTE");
                                } else {
                                    // mode invalide mais here we want valid payments mostly
                                    try { service.payer(b, "INVALID_MODE"); } catch (Exception ex) { /* ignore */ }
                                }
                            } catch (PaiementInvalideException pe) {
                                // paiement échoué - on continue
                            }
                            billetsVendusCodes.add(b.getCodeBillet());
                        } catch (Exception ex) {
                            // ignorer erreurs lors de simulation de masse
                        }
                    }
                    if (billetsVendusCodes.size() >= ventesCible) break;
                }
            }

            System.out.println("Vente simulée: " + billetsVendusCodes.size() + " billets réservés/émis");

            // ------------------ ANNULATIONS: annuler au moins 5 billets (dont 2 <24h) ------------------
            int annulations = 0;
            for (int i = 0; i < Math.min(10, billetsVendusCodes.size()) && annulations < 5; i++) {
                String code = billetsVendusCodes.get(i);
                try {
                    service.annulerBillet(code);
                    annulations++;
                } catch (Exception e) {
                    // ignorer
                }
            }
            System.out.println("Annulations simulées: " + annulations);

            // ------------------ PROVOQUER ERREURS ------------------
            // 1) Quota épuisé: créer petite zone (capacité 1) et tenter 2 réservations
            Match m0 = createdMatches.get(0);
            try {
                ZonePlace tiny = new ZonePlace(ZoneType.ZONE3, 1, ZoneType.ZONE3.getDefaultPrice());
                service.ajouterZonePourMatch(m0, tiny);
                // première réservation ok
                Client c1 = service.getClients().get(0);
                service.reserverBillet(c1, m0, tiny);
                // seconde réservation doit lever BilletIndisponibleException
                try {
                    service.reserverBillet(service.getClients().get(1), m0, tiny);
                } catch (BilletIndisponibleException bie) {
                    System.out.println("Erreur attendue (quota épuisé): " + bie.getMessage());
                }
            } catch (DonneeInvalideException dive) {
                System.out.println("Erreur lors création tiny zone: " + dive.getMessage());
            }

            // 2) Paiement invalide: essayer de payer avec mode invalide
            try {
                // prendre un billet existant (le premier payé ou réservé)
                if (!service.getBillets().isEmpty()) {
                    Billet some = service.getBillets().get(0);
                    try {
                        service.payer(some, "BITCOIN");
                    } catch (PaiementInvalideException pi) {
                        System.out.println("Erreur attendue (paiement invalide): " + pi.getMessage());
                    }
                }
            } catch (Exception e) { }

            // 3) Accreditation refusée: tenter de réserver pour un média non accrédité
            try {
                // trouver un media non accrédité
                Client mediaClient = null;
                for (Client c : service.getClients()) if (c instanceof Media) { mediaClient = c; break; }
                if (mediaClient != null) {
                    Match m1 = createdMatches.get(1);
                    ZonePlace z = service.getZonesParMatch().get(m1).get(0);
                    try {
                        service.reserverBillet(mediaClient, m1, z);
                    } catch (AccreditationRefuseeException are) {
                        System.out.println("Erreur attendue (accréditation refusée): " + are.getMessage());
                    }
                }
            } catch (Exception e) { }

            // 4) Données invalides: tenter d'ajouter un match en double
            try {
                Match dup = new Match("Maroc", "Comores", Stade.RABAT_MOULAY_ABDELLAH, makeDate.apply("2025-12-21", "20:00"), mapImportance.apply(2.0));
                try { service.ajouterMatch(dup); } catch (DonneeInvalideException di) {
                    System.out.println("Erreur attendue (données invalides / doublon): " + di.getMessage());
                }
            } catch (DonneeInvalideException e) { }

            // ------------------ GÉNÉRER RAPPORTS ------------------
            service.genererRapportVentes();
            service.exporterRapportTXT("exports/rapport_ventes.txt");
        
        System.out.print("Choisissez une zone (numéro): ");
        int choixZone = Integer.parseInt(scanner.nextLine()) - 1;
        
        if (choixZone < 0 || choixZone >= zones.size()) {
            System.out.println("Choix invalide!");
            return;
        }
        
        ZonePlace zone = zones.get(choixZone);
        
        try {
            // Réserver le billet
            Billet billet = service.reserverBillet(client, match, zone);
            System.out.println("Billet réservé avec succès: " + billet.getCodeBillet());
            System.out.printf("Prix du billet: %.2f DH%n", billet.getMontant());

            // Paiement
            System.out.print("Mode de paiement (CARTE/ESPECES/VIREMENT): ");
            String modePaiement = scanner.nextLine().toUpperCase();

            try {
                service.payer(billet, modePaiement);
                System.out.println("Paiement confirmé! Billet émis: " + billet.getCodeBillet());
            } catch (PaiementInvalideException e) {
                System.out.println("Erreur de paiement: " + e.getMessage());
            }
            
        } catch (BilletIndisponibleException | DonneeInvalideException | AccreditationRefuseeException e) {
            System.out.println("Erreur lors de la réservation: " + e.getMessage());
        }
    }
    
    private static void annulerBillet() {
        System.out.println("\n--- Annulation d'un billet ---");
        
        System.out.print("Code du billet à annuler: ");
        String codeBillet = scanner.nextLine();
        
        try {
            service.annulerBillet(codeBillet);
        } catch (DonneeInvalideException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }
    
    private static void afficherBilletsClient() {
        System.out.println("\n--- Billets d'un client ---");
        
        System.out.print("ID ou email du client: ");
        String identifiant = scanner.nextLine();
        
        List<Client> clients = service.getClients();
        Client clientTrouve = null;
        
        // Recherche par ID ou email
        for (Client client : clients) {
            if (String.valueOf(client.getId()).equals(identifiant) || 
                client.getEmail().equalsIgnoreCase(identifiant)) {
                clientTrouve = client;
                break;
            }
        }
        
        if (clientTrouve == null) {
            System.out.println("Client non trouvé!");
            return;
        }
        
        List<Billet> billetsClient = service.getBilletsParClient(clientTrouve);
        
        System.out.println("\nBillets de " + clientTrouve.getNom() + ":");
        if (billetsClient.isEmpty()) {
            System.out.println("Aucun billet trouvé.");
            // Ne rien exporter si pas de billets
            return;
        } else {
            for (Billet billet : billetsClient) {
                System.out.println("  - " + billet);
            }
            // Export CSV and JSON
            try {
                writeBilletsExport(billetsClient, clientTrouve);
            } catch (IOException e) {
                System.out.println("Erreur d'export des billets: " + e.getMessage());
            }
        }
    }

    private static void writeBilletsExport(List<Billet> billets, Client client) throws IOException {
        if (billets == null || billets.isEmpty()) return;

        Path exportsDir = Paths.get("exports");
        if (!Files.exists(exportsDir)) Files.createDirectories(exportsDir);

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String idPart = String.valueOf(client.getId());
        String baseName = "billets_" + idPart + "_" + stamp;

        // CSV
        Path csvPath = exportsDir.resolve(baseName + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("Code;ClientId;ClientNom;ClientEmail;MatchCode;Equipe1;Equipe2;Stade;Zone;Statut;Montant;DateReservation");
            writer.newLine();
            DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (Billet b : billets) {
                String line = String.join(";",
                    safe(b.getCodeBillet()),
                    String.valueOf(b.getClient().getId()),
                    safe(b.getClient().getNom()),
                    safe(b.getClient().getEmail()),
                    safe(b.getMatch().getCodeMatch()),
                    safe(b.getMatch().getEquipe1()),
                    safe(b.getMatch().getEquipe2()),
                    safe(b.getMatch().getStade().toString()),
                    safe(b.getZone().getNomZone()),
                    safe(b.getStatut()),
                    String.format(Locale.ROOT, "%.2f", b.getMontant()),
                    b.getDateReservation() != null ? b.getDateReservation().format(df) : ""
                );
                writer.write(line);
                writer.newLine();
            }
        }

        // JSON
        Path jsonPath = exportsDir.resolve(baseName + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            writer.write("[");
            writer.newLine();
            DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (int i = 0; i < billets.size(); i++) {
                Billet b = billets.get(i);
                StringBuilder obj = new StringBuilder();
                obj.append("  {");
                obj.append("\"code\":\"").append(escapeJson(b.getCodeBillet())).append("\",");
                obj.append("\"clientId\":").append(b.getClient().getId()).append(",");
                obj.append("\"clientNom\":\"").append(escapeJson(b.getClient().getNom())).append("\",");
                obj.append("\"clientEmail\":\"").append(escapeJson(b.getClient().getEmail())).append("\",");
                obj.append("\"matchCode\":\"").append(escapeJson(b.getMatch().getCodeMatch())).append("\",");
                obj.append("\"equipe1\":\"").append(escapeJson(b.getMatch().getEquipe1())).append("\",");
                obj.append("\"equipe2\":\"").append(escapeJson(b.getMatch().getEquipe2())).append("\",");
                obj.append("\"stade\":\"").append(escapeJson(b.getMatch().getStade().toString())).append("\",");
                obj.append("\"zone\":\"").append(escapeJson(b.getZone().getNomZone())).append("\",");
                obj.append("\"statut\":\"").append(escapeJson(b.getStatut())).append("\",");
                obj.append("\"montant\":").append(String.format(Locale.ROOT, "%.2f", b.getMontant())).append(",");
                obj.append("\"dateReservation\":\"").append(b.getDateReservation() != null ? escapeJson(b.getDateReservation().format(df)) : "").append("\"");
                obj.append("  }");
                if (i < billets.size() - 1) obj.append(",");
                writer.write(obj.toString());
                writer.newLine();
            }
            writer.write("]");
            writer.newLine();
        }

        System.out.println("Billets exportés: ");
        System.out.println(" - CSV: " + csvPath.toAbsolutePath());
        System.out.println(" - JSON: " + jsonPath.toAbsolutePath());
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace(";", ",");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    private static void genererRapports() {
        System.out.println("\n--- Rapports ---");
        
        service.genererRapportVentes();
        // Exporter le rapport dans un fichier texte
        String chemin = "exports/rapport_ventes.txt";
        service.exporterRapportTXT(chemin);
        System.out.println("Rapport exporté dans: " + chemin);
    }
    
    // ==== INITIALISATION DES DONNÉES DE TEST ====
    
    private static void initialiserDonneesTest() {
        System.out.println("Initialisation des données de test...");
        
        try {
            // Création de 8 matchs (exigence)
            LocalDateTime baseDate = LocalDateTime.now().plusDays(7);
            
            model.Stade[] stades = model.Stade.values();
            for (int i = 1; i <= 8; i++) {
                model.Stade s = stades[(i-1) % stades.length];
                Match match = new Match(
                    "Équipe A" + i,
                    "Équipe B" + i,
                    s,
                    baseDate.plusDays(i),
                    (i % 3) + 1
                );
                service.ajouterMatch(match);
                
                // Ajouter 3 zones par match (ZONE1..ZONE3)
                model.ZoneType[] ztypes = model.ZoneType.values();
                for (int j = 1; j <= 3; j++) {
                    model.ZoneType zt = ztypes[j % ztypes.length];
                    double price = zt.getDefaultPrice();
                    ZonePlace zone = new ZonePlace(
                        zt,
                        100 * j,
                        price
                    );
                    service.ajouterZonePourMatch(match, zone);
                }
            }
            
            // Création de 25 clients (dont au moins 5 médias)
            // 20 spectateurs
            for (int i = 1; i <= 20; i++) {
                Spectateur spectateur = new Spectateur(
                    "Spectateur " + i,
                    "spectateur" + i + "@email.com",
                    i % 3 == 0 // 1/3 sont fidèles
                );
                service.ajouterClient(spectateur);
            }
            
            // 5 médias
            String[] typesMedia = {"TV", "Radio", "Presse", "Web", "TV"};
            for (int i = 1; i <= 5; i++) {
                Media media = new Media(
                    "Média " + i,
                    "media" + i + "@press.com",
                    typesMedia[i-1]
                );
                service.ajouterClient(media);
            }
            
            System.out.println("Données de test initialisées:");
            System.out.println("- 8 matchs créés");
            System.out.println("- 25 clients créés (20 spectateurs, 5 médias)");
            
        } catch (DonneeInvalideException e) {
            System.out.println("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }
}