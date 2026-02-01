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
        
        // Charger les matchs sauvegardés si présents, sinon initialiser les données de test
        service.chargerMatchsDepuisJSON("exports/matches.json");
        if (service.getMatchs().isEmpty()) {
            initialiserDonneesTest();
        } else {
            System.out.println("Matchs chargés depuis exports/matches.json: " + service.getMatchs().size());
        }
        // Charger les billets précédemment sauvegardés (persistés)
        service.chargerBilletsDepuisJSON("exports/billets_reserves.json");
        
        boolean continuer = true;
        while (continuer) {
            afficherMenu();
            int choix = lireChoix();
            
            try {
                switch (choix) {
                    case 1:
                        ajouterMatch();
                        break;
                    case 2:
                        ajouterZone();
                        break;
                    case 3:
                        ajouterClient();
                        break;
                    case 4:
                        accrediterMedia();
                        break;
                    case 5:
                        vendreBillet();
                        break;
                    case 6:
                        annulerBillet();
                        break;
                    case 7:
                        afficherBilletsClient();
                        break;
                    case 8:
                        genererRapports();
                        break;
                    case 10:
                        afficherClients();
                        break;
                    case 11:
                        reinitialiserClients();
                        break;
                    case 9:
                        continuer = false;
                        System.out.println("Au revoir!");
                        break;
                    default:
                        System.out.println("Choix invalide!");
                        break;
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
        System.out.println("10. Lister les clients");
        System.out.println("11. Réinitialiser les clients (supprimer anciens et créer nouveaux)");
        System.out.println("9. Quitter");
        System.out.print("Votre choix: ");
    }
    
    private static int lireChoix() {
        try {
            if (!scanner.hasNextLine()) return -1;
            String line = scanner.nextLine();
            return Integer.parseInt(line);
        } catch (NumberFormatException | java.util.NoSuchElementException | IllegalStateException e) {
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

            // Construire l'objet LocalDateTime
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);
            int hh = Integer.parseInt(hourPart);
            int mm = Integer.parseInt(minutePart);
            LocalDateTime dateHeure = LocalDateTime.of(y, m, d, hh, mm);

            // Demander l'importance du match
            System.out.print("Importance du match (1.0=Normal, 1.5=Important, 2.0=Très important) [1.0]: ");
            String impStr = scanner.nextLine().trim();
            double importance = 1.0;
            if (!impStr.isEmpty()) {
                try {
                    importance = Double.parseDouble(impStr);
                } catch (NumberFormatException ex) {
                    importance = 1.0;
                }
            }

            // Créer le match
            Match match = new Match(equipe1, equipe2, stadeObj, dateHeure, importance);
            // Par défaut, laisser la capacité du match égale à la capacité du stade
            match.setCapacite(stadeObj.getCapacite());
            service.ajouterMatch(match);
            System.out.println("Match créé: " + match.getCodeMatch() + " - " + match.getEquipe1() + " vs " + match.getEquipe2());
            // Sauvegarder les matchs après création pour persistance entre les runs
            try {
                java.nio.file.Path exportsDir = java.nio.file.Paths.get("exports");
                if (!java.nio.file.Files.exists(exportsDir)) java.nio.file.Files.createDirectories(exportsDir);
                service.sauvegarderMatchs("exports/matches.json");
                System.out.println("Matchs sauvegardés dans exports/matches.json");
            } catch (Exception e) {
                System.out.println("Échec de la sauvegarde des matchs: " + e.getMessage());
            }
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

        // 1. Choisir un client — permettre à l'utilisateur de choisir d'afficher
        // uniquement les spectateurs ou tous les clients (incluant les médias)
        List<Client> allClients = service.getClients();
        if (allClients.isEmpty()) {
            System.out.println("Aucun client disponible. Ajoutez d'abord un client.");
            return;
        }

        System.out.println("Afficher : 1) Spectateurs uniquement  2) Tous les clients");
        int affichageChoix = 1;
        try {
            System.out.print("Votre choix (1-2) [1]: ");
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) affichageChoix = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            affichageChoix = 1;
        }

        List<Client> listeSelection = new ArrayList<>();
        if (affichageChoix == 2) {
            // Tous les clients
            listeSelection.addAll(allClients);
            System.out.println("Clients disponibles:");
        } else {
            // Seulement les spectateurs
            for (Client c : allClients) if (c instanceof Spectateur) listeSelection.add(c);
            System.out.println("Spectateurs disponibles:");
        }

        if (listeSelection.isEmpty()) {
            System.out.println("Aucun client correspondant au filtre. Ajoutez d'abord un client approprié.");
            return;
        }

        for (int i = 0; i < listeSelection.size(); i++) {
            System.out.println((i+1) + ". " + listeSelection.get(i));
        }
        System.out.print("Choisissez un client (numéro): ");
        int choixClient = -1;
        try { choixClient = Integer.parseInt(scanner.nextLine()) - 1; } catch (NumberFormatException e) { choixClient = -1; }
        if (choixClient < 0 || choixClient >= listeSelection.size()) {
            System.out.println("Choix invalide!");
            return;
        }
        Client client = listeSelection.get(choixClient);

        // 2. Choisir un match
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

        // 3. Choisir une zone
        List<ZonePlace> zones = service.getZonesParMatch().get(match);
        if (zones == null || zones.isEmpty()) {
            System.out.println("Aucune zone disponible pour ce match.");
            return;
        }
        System.out.println("Zones disponibles:");
        for (int i = 0; i < zones.size(); i++) {
            System.out.println((i+1) + ". " + zones.get(i));
        }
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

            // Paiement avec enum
            System.out.println("Mode de paiement :");
            String[] modes = {"CARTE", "ESPECES", "VIREMENT"};
            for (int i = 0; i < modes.length; i++) {
                System.out.println((i+1) + ". " + modes[i]);
            }
            int choixMode = -1;
            while (choixMode < 1 || choixMode > modes.length) {
                System.out.print("Choisissez le mode de paiement (numéro): ");
                try {
                    choixMode = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    choixMode = -1;
                }
            }
            String modePaiement = modes[choixMode-1];

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

    private static void afficherClients() {
        System.out.println("\n--- Liste des clients ---");
        List<Client> clients = service.getClients();
        if (clients.isEmpty()) {
            System.out.println("Aucun client enregistré.");
            return;
        }
        int nbSpect = 0, nbMedia = 0;
        for (Client c : clients) {
            System.out.println(" - " + c);
            if (c instanceof Spectateur) nbSpect++; else if (c instanceof Media) nbMedia++;
        }
        System.out.println(String.format("Total: %d clients (%d spectateurs, %d médias)", clients.size(), nbSpect, nbMedia));
    }
    
    // ==== INITIALISATION DES DONNÉES DE TEST ====
    
    private static void initialiserDonneesTest() {
        System.out.println("Initialisation des données de test...");
        try {
            // Suppression des anciens matchs et création des 10 matchs demandés
            service.getMatchs().clear();
            Object[][] matchsInfos = {
                {"Maroc", "Zambie", "2025-01-01T20:00", "Prince Moulay Abdellah", 1.5},
                {"Nigeria", "Guinée équatoriale", "2025-01-02T17:00", "Rabat madina", 1.0},
                {"Égypte", "Ghana", "2025-01-05T20:00", "Fès", 1.0},
                {"Maroc", "Cameroun", "2025-01-05T20:00", "Prince Moulay Abdellah", 1.5},
                {"Sénégal", "Cameroun", "2025-01-07T18:00", "Agadir", 1.5},
                {"Maroc", "Mali", "2025-01-08T21:00", "Prince Moulay Abdellah", 1.5},
                {"Algérie", "Nigeria", "2025-01-09T20:00", "Fès", 1.5},
                {"Maroc", "Sénégal", "2025-01-15T21:00", "Mohammed V", 2.0},
                {"Nigeria", "Égypte", "2025-01-13T20:00", "Casablanca", 1.5},
                {"Maroc", "Nigeria", "2025-02-20T21:00", "Prince Moulay Abdellah", 2.0}
            };
            for (Object[] info : matchsInfos) {
                String eq1 = (String) info[0];
                String eq2 = (String) info[1];
                LocalDateTime date = LocalDateTime.parse((String) info[2]);
                String stadeNom = (String) info[3];
                double importance = (double) info[4];
                model.Stade stade = null;
                for (model.Stade s : model.Stade.values()) {
                    if (s.toString().equalsIgnoreCase(stadeNom) || s.name().replace("_", " ").equalsIgnoreCase(stadeNom.replace(" ", "_"))) {
                        stade = s;
                        break;
                    }
                }
                if (stade == null) stade = model.Stade.values()[0]; // fallback
                // Capacité aléatoire entre 20000 et 60000
                int capaciteMatch = 20000 + (int)(Math.random() * (60000 - 20000 + 1));
                Match match = new Match(eq1, eq2, stade, date, importance);
                match.setCapacite(capaciteMatch);
                service.ajouterMatch(match);
                // Zones avec répartition 40%/50%/10%
                // Forcer l'ordre : Zone 1, Zone 2, VIP
                ZoneType zone1Type = null, zone2Type = null, vipType = null;
                for (ZoneType zt : model.ZoneType.values()) {
                    String name = zt.name().toUpperCase();
                    if (name.contains("1")) zone1Type = zt;
                    else if (name.contains("2")) zone2Type = zt;
                    else if (name.contains("3")) vipType = zt;
                }
                int cap1 = (int)(capaciteMatch * 0.4);
                int cap2 = (int)(capaciteMatch * 0.5);
                int capVIP = capaciteMatch - cap1 - cap2;
                double price1 = (zone1Type != null) ? zone1Type.getDefaultPrice() : 50.0;
                double price2 = (zone2Type != null) ? zone2Type.getDefaultPrice() : 75.0;
                double priceVIP = (vipType != null) ? vipType.getDefaultPrice() : 150.0;
                if (zone1Type != null) service.ajouterZonePourMatch(match, new ZonePlace(zone1Type, cap1, price1));
                if (zone2Type != null) service.ajouterZonePourMatch(match, new ZonePlace(zone2Type, cap2, price2));
                if (vipType != null) service.ajouterZonePourMatch(match, new ZonePlace(vipType, capVIP, priceVIP));
            }

            // Supprimer tous les clients existants
            service.getClients().clear();

            // 25 spectateurs marocains
            String[] nomsSpectateurs = {
                "Youssef El Amrani", "Fatima Zahra Benali", "Mohamed Idrissi", "Khadija El Hajj",
                "Hassan Ouazzani", "Imane El Khatib", "Rachid Bekkali", "Zineb Chraibi",
                "Omar El Fassi", "Najat El Idrissi", "Soufiane Bennis", "Leila Bourhim",
                "Adil Saidi", "Meryem Haddad", "Anas El Mansouri", "Nour El Khattabi",
                "Karim Azmani", "Samira El Omari", "Reda Amghar", "Sara Amrani",
                "Hicham El Alaoui", "Salma Bouzid", "Walid El Fadili", "Siham El Gharbi", "Amine Chafai"
            };
            Set<String> nomsSpectateursSet = new HashSet<>(Arrays.asList(nomsSpectateurs));
            String[] nomsMedias = {"Salim Azizi", "Rida Hasnaoui", "Amine Bidouni", "Safae Jarmouni", "Ahlam Dani"};
            String[] typesMedia = {"TV", "Radio", "Presse", "Web", "Presse"};
            for (int i = 0; i < nomsMedias.length; i++) {
                String nom = nomsMedias[i];
                String email = nom.toLowerCase().replace(" ", ".") + "@press.ma";
                Media media = new Media(nom, email, typesMedia[i]);
                service.ajouterClient(media);
            }

            for (int i = 0; i < nomsMedias.length; i++) {
                String nom = nomsMedias[i];
                String email = nom.toLowerCase().replace(" ", ".") + "@press.ma";
                Media media = new Media(nom, email, typesMedia[i]);
                service.ajouterClient(media);
            }
            Set<String> nomsMediasSet = new HashSet<>(Arrays.asList(nomsMedias));

            for (int i = 0; i < nomsSpectateurs.length; i++) {
                String nom = nomsSpectateurs[i];
                String email = nom.toLowerCase().replace(" ", ".") + "@mail.ma";
                boolean fidele = (i % 4 == 0);
                Spectateur spectateur = new Spectateur(nom, email, fidele);
                service.ajouterClient(spectateur);
            }

            for (int i = 0; i < nomsMedias.length; i++) {
                String nom = nomsMedias[i];
                String email = "media" + (i+1) + "@press.ma";
                Media media = new Media(nom, email, typesMedia[i]);
                service.ajouterClient(media);
            }

            // Filtrer pour ne garder que les clients marocains
            List<Client> clients = service.getClients();
            clients.removeIf(c -> {
                String nom = c.getNom();
                return !(nomsSpectateursSet.contains(nom) || nomsMediasSet.contains(nom));
            });

            System.out.println("Données de test initialisées:");
            System.out.println("- " + service.getMatchs().size() + " matchs créés");
            int nbSpectateurs = 0, nbMedias = 0;
            for (Client c : service.getClients()) {
                if (c instanceof model.Spectateur) nbSpectateurs++;
                else if (c instanceof model.Media) nbMedias++;
            }
            System.out.println("- " + service.getClients().size() + " clients marocains (" + nbSpectateurs + " spectateurs, " + nbMedias + " médias)");
        } catch (DonneeInvalideException e) {
            System.out.println("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    private static void reinitialiserClients() {
        System.out.println("Réinitialisation des clients: suppression des anciens et création des nouveaux clients...");
        try {
            // Supprimer tous les clients existants
            service.getClients().clear();

            // 25 spectateurs marocains
            String[] nomsSpectateurs = {
            "Youssef El Amrani", "Fatima Zahra Benali", "Mohamed Idrissi", "Khadija El Hajj",
            "Hassan Ouazzani", "Imane El Khatib", "Rachid Bekkali", "Zineb Chraibi",
            "Omar El Fassi", "Najat El Idrissi", "Soufiane Bennis", "Leila Bourhim",
            "Adil Saidi", "Meryem Haddad", "Anas El Mansouri", "Nour El Khattabi",
            "Karim Azmani", "Samira El Omari", "Reda Amghar", "Sara Amrani",
            "Hicham El Alaoui", "Salma Bouzid", "Walid El Fadili", "Siham El Gharbi", "Amine Chafai"
        };

        // 5 médias marocains
        String[] nomsMedias = {"Salim Azizi", "Rida Hasnaoui", "Amine Bidouni", "Safae Jarmouni", "Ahlam Dani"};
        String[] typesMedia = {"TV", "Radio", "Presse", "Web", "Presse"};

        // Ajouter médias
        for (int i = 0; i < nomsMedias.length; i++) {
            String nom = nomsMedias[i];
            String email = nom.toLowerCase().replace(" ", ".") + "@press.ma";
            Media media = new Media(nom, email, typesMedia[i]);
            service.ajouterClient(media);
        }

        // Ajouter spectateurs
        for (int i = 0; i < nomsSpectateurs.length; i++) {
            String nom = nomsSpectateurs[i];
            String email = nom.toLowerCase().replace(" ", ".") + "@mail.ma";
            boolean fidele = (i % 4 == 0);
            Spectateur s = new Spectateur(nom, email, fidele);
            service.ajouterClient(s);
        }

        // Sauvegarder les clients dans le fichier JSON
        try {
            java.nio.file.Path exportsDir = java.nio.file.Paths.get("exports");
            if (!java.nio.file.Files.exists(exportsDir)) java.nio.file.Files.createDirectories(exportsDir);
            service.sauvegarderClients("exports/clients.json");
            System.out.println("Clients réinitialisés et sauvegardés dans exports/clients.json");
        } catch (Exception e) {
            System.out.println("Échec de la sauvegarde des clients: " + e.getMessage());
        }
        } catch (DonneeInvalideException e) {
            System.out.println("Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }
}