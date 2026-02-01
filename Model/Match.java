package model;

import java.time.LocalDateTime;
import exceptions.DonneeInvalideException;

import model.Stade;

public class Match {
    private static int compteurCode = 1;
    private final String codeMatch;
    private String equipe1;
    private String equipe2;
    private Stade stade;
    private LocalDateTime dateHeure;
    private double importance; // 1: Normal, 2: Important, 3: Très important
    private int capacite; // capacité effective pour le match (peut être inférieure au stade)
    
    public Match(String equipe1, String equipe2, Stade stade, 
                 LocalDateTime dateHeure, double importance) throws DonneeInvalideException {
        
        if (equipe1 == null || equipe2 == null || stade == null || dateHeure == null) {
            throw new DonneeInvalideException("Tous les champs du match doivent être renseignés");
        }
        if (importance < 1 || importance > 3) {
            throw new DonneeInvalideException("L'importance doit être entre 1 et 3");
        }
        
        this.codeMatch = "MAT" + String.format("%03d", compteurCode++);
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.stade = stade;
        this.dateHeure = dateHeure;
        this.importance = importance;
        // Par défaut, la capacité du match est la capacité du stade
        this.capacite = stade.getCapacite();
    }
    
    // Getters
    public String getCodeMatch() { return codeMatch; }
    public String getEquipe1() { return equipe1; }
    public String getEquipe2() { return equipe2; }
    public Stade getStade() { return stade; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public double getImportance() { return importance; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    
    public double getCoefficientImportance() {
        if (importance == 1.0) {
            return 1.0;
        } else if (importance == 1.5) {
            return 1.5;
        } else if (importance == 2.0) {
            return 2.0;
        } else {
            return 1.0;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Match[Code=%s, %s vs %s, Stade=%s, Date=%s, Importance=%.1f, Capacité=%d]", 
            codeMatch, equipe1, equipe2, stade.toString(), dateHeure.toString(), importance, capacite);
    }
}