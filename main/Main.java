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
        List<Client> clients = service.getClients();
        if (clients.isEmpty()) {
            System.out.println("Aucun client disponible. Ajoutez d'abord des clients.");
            return;
        }
        
        System.out.println("Clients disponibles:");
        for (int i = 0; i < clients.size(); i++) {
            System.out.println((i+1) + ". " + clients.get(i));
        }
        
        System.out.print("Choisissez un client (numéro): ");
        int choixClient = Integer.parseInt(scanner.nextLine()) - 1;
        
        if (choixClient < 0 || choixClient >= clients.size()) {
            System.out.println("Choix invalide!");
            return;
        }
        
        Client client = clients.get(choixClient);
        
        // Choisir un match
        List<Match> matchs = service.getMatchs();
        if (matchs.isEmpty()) {
            System.out.println("Aucun match disponible.");
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
        
        // Choisir une zone
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