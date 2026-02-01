package model;

import exceptions.DonneeInvalideException;

public class ZonePlace {
    private ZoneType type;
    private int capacite;
    private double prix; // prix de base pour la zone
    private int placesVendues; // Pour gérer le quota

    public ZonePlace(ZoneType type, int capacite, double prix) throws DonneeInvalideException {
        if (type == null) {
            throw new DonneeInvalideException("Le type de zone ne peut pas être null");
        }
        if (capacite <= 0) {
            throw new DonneeInvalideException("La capacité doit être positive");
        }
        if (prix < 0) {
            throw new DonneeInvalideException("Le prix doit être positif ou nul");
        }

        this.type = type;
        this.capacite = capacite;
        this.prix = prix;
        this.placesVendues = 0;
    }
    
    public boolean estDisponible() {
        return placesVendues < capacite;
    }
    
    public void incrementerVentes() {
        if (placesVendues < capacite) {
            placesVendues++;
        }
    }
    
    public void decrementerVentes() {
        if (placesVendues > 0) {
            placesVendues--;
        }
    }
    
    // Getters
    public String getNomZone() {
        // Afficher VIP si c'est la 3e zone (ZONE3)
        if (type.name().equalsIgnoreCase("ZONE3")) {
            return "VIP";
        }
        return type.toString();
    }
    public int getCapacite() { return capacite; }
    public double getPrix() { return prix; }
    public ZoneType getType() { return type; }
    public double getCoefficientPrix() { return prix; }
    public int getPlacesVendues() { return placesVendues; }
    public int getPlacesRestantes() { return capacite - placesVendues; }
    
    @Override
    public String toString() {
        return String.format("Zone[Nom=%s, Capacité=%d, Vendu=%d, Restant=%d, Prix=%.2f]", 
            getNomZone(), capacite, placesVendues, getPlacesRestantes(), prix);
    }
}