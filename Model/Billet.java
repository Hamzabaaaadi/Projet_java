package model;

import java.time.LocalDateTime;

public class Billet {
    private static int compteurCode = 1;
    private final String codeBillet;
    private Client client;
    private Match match;
    private ZonePlace zone;
    private String statut; // "RESERVE", "CONFIRME", "ANNULE"
    private double montant;
    private LocalDateTime dateReservation;
    
    public Billet(Client client, Match match, ZonePlace zone, double montant) {
        this.codeBillet = "BIL" + String.format("%06d", compteurCode++);
        this.client = client;
        this.match = match;
        this.zone = zone;
        this.statut = "RESERVE";
        this.montant = montant;
        this.dateReservation = LocalDateTime.now();
    }
    
    // Getters et setters
    public String getCodeBillet() { return codeBillet; }
    public Client getClient() { return client; }
    public Match getMatch() { return match; }
    public ZonePlace getZone() { return zone; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public LocalDateTime getDateReservation() { return dateReservation; }
    
    @Override
    public String toString() {
        return String.format("Billet[Code=%s, Client=%s, Match=%s, Zone=%s, Statut=%s, Montant=%.2f, Date=%s]", 
            codeBillet, client.getNom(), match.getCodeMatch(), zone.getNomZone(), 
            statut, montant, dateReservation.toString());
    }
}