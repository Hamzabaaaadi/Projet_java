package model;

import exceptions.DonneeInvalideException;

public class Spectateur extends Client {
    private static final double REDUCTION_STANDARD = 0.0; // 0% de réduction
    private static final double REDUCTION_FIDELE = 0.10; // 10% de réduction
    private boolean fidele;
    
    public Spectateur(String nom, String email, boolean fidele) throws DonneeInvalideException {
        super(nom, email);
        this.fidele = fidele;
    }
    
    @Override
    public double getReduction() {
        return fidele ? REDUCTION_FIDELE : REDUCTION_STANDARD;
    }
    
    public boolean isFidele() { return fidele; }
    
    @Override
    public String toString() {
        return String.format("Spectateur[ID=%d, Nom=%s, Email=%s, Réduction=%.2f%%, Fidèle=%s]",
            getId(), getNom(), getEmail(), getReduction() * 100, fidele);
    }
}