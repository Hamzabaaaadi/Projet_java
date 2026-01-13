package model;

import java.util.UUID;
import exceptions.DonneeInvalideException;

public abstract class Client {
    private static int compteurId = 1;
    private final int id;
    private String nom;
    private String email;
    
    public Client(String nom, String email) throws DonneeInvalideException {
        if (nom == null || nom.trim().isEmpty()) {
            throw new DonneeInvalideException("Le nom ne peut pas être vide");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new DonneeInvalideException("Email invalide");
        }
        
        this.id = compteurId++;
        this.nom = nom;
        this.email = email;
    }
    
    // Méthode abstraite pour le polymorphisme
    public abstract double getReduction();
    
    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    
    @Override
    public String toString() {
        return String.format("Client[ID=%d, Nom=%s, Email=%s, Réduction=%.2f%%]", 
            id, nom, email, getReduction() * 100);
    }
}