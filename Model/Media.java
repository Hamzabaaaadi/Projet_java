package model;

import exceptions.DonneeInvalideException;

public class Media extends Client {
    private boolean accreditationValidee;
    private String typeMedia; // "TV", "Radio", "Presse", "Web"
    
    public Media(String nom, String email, String typeMedia) throws DonneeInvalideException {
        super(nom, email);
        this.typeMedia = typeMedia;
        this.accreditationValidee = false; // Par défaut non accrédité
    }
    
    @Override
    public double getReduction() {
        // Médias accrédités ont 100% de réduction (gratuit)
        // Sinon 50% de réduction
        return accreditationValidee ? 1.0 : 0.5;
    }
    
    // Getters et setters
    public boolean isAccreditationValidee() { return accreditationValidee; }
    public void setAccreditationValidee(boolean accreditationValidee) { 
        this.accreditationValidee = accreditationValidee; 
    }
    public String getTypeMedia() { return typeMedia; }
    
    @Override
    public String toString() {
        return "Media" + super.toString().substring(6) + 
               String.format(", Type=%s, Accrédité=%s", typeMedia, accreditationValidee);
    }
}