package model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Paiement {
    private final String reference;
    private Billet billet;
    private String modePaiement; // "CARTE", "ESPECES", "VIREMENT"
    private String statut; // "EN_ATTENTE", "PAYE", "REFUSE"
    private LocalDateTime dateHeure;
    
    public Paiement(Billet billet, String modePaiement) {
        this.reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        this.billet = billet;
        this.modePaiement = modePaiement;
        this.statut = "EN_ATTENTE";
        this.dateHeure = LocalDateTime.now();
    }
    
    // Getters et setters
    public String getReference() { return reference; }
    public Billet getBillet() { return billet; }
    public String getModePaiement() { return modePaiement; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    
    @Override
    public String toString() {
        return String.format("Paiement[Réf=%s, Billet=%s, Mode=%s, Statut=%s, Date=%s]", 
            reference, billet.getCodeBillet(), modePaiement, statut, dateHeure.toString());
    }
}