package model;

public enum Stade {
    RABAT_MOULAY_ABDELLAH("Rabat Moulay Abdellah"),
    RABAT_BARID("Rabat Barid", 20000),
    CASABLANCA("Casablanca", 70000),
    TANGER("Tanger", 40000),
    FES("Fes", 35000),
    AGADIR("Agadir", 45000),
    MARRAKECH("Marrakech", 45000);

    private final String display;
    private final int capacite;
    Stade(String display) { this(display, 40000); }
    Stade(String display, int capacite) { this.display = display; this.capacite = capacite; }

    @Override
    public String toString() { return display; }

    public int getCapacite() { return capacite; }

    public static Stade fromString(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        switch (t) {
            case "rabat moulay abdellah": case "rabat_moulay_abdellah": case "rabat moulay": case "rabatmoulayabdellah": return RABAT_MOULAY_ABDELLAH;
            case "rabat barid": case "rabat_barid": case "rabatbarid": return RABAT_BARID;
            case "casablanca": case "casa": return CASABLANCA;
            case "tanger": return TANGER;
            case "fes": return FES;
            case "agadir": return AGADIR;
            case "marrakech": case "marackech": return MARRAKECH;
            default:
                // try match by prefix
                for (Stade st : values()) {
                    if (st.display.toLowerCase().startsWith(t) || st.name().toLowerCase().startsWith(t)) return st;
                }
                return null;
        }
    }
}
