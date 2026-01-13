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
    private int importance; // 1: Normal, 2: Important, 3: Très important
    
    public Match(String equipe1, String equipe2, Stade stade, 
                 LocalDateTime dateHeure, int importance) throws DonneeInvalideException {
        
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
    }
    
    // Getters
    public String getCodeMatch() { return codeMatch; }
    public String getEquipe1() { return equipe1; }
    public String getEquipe2() { return equipe2; }
    public Stade getStade() { return stade; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public int getImportance() { return importance; }
    
    public double getCoefficientImportance() {
        return switch (importance) {
            case 1 -> 1.0;
            case 2 -> 1.5;
            case 3 -> 2.0;
            default -> 1.0;
        };
    }
    
    @Override
    public String toString() {
        return String.format("Match[Code=%s, %s vs %s, Stade=%s, Date=%s, Importance=%d]", 
            codeMatch, equipe1, equipe2, stade.toString(), dateHeure.toString(), importance);
    }
}