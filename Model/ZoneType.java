package model;

public enum ZoneType {
    VIP("VIP", 150.0),
    ZONE1("Zone 1", 50.0),
    ZONE2("Zone 2", 75.0),
    ZONE3("Zone 3", 100.0),
    ZONE4("Zone 4", 125.0),
    ZONE5_ULTRAS("Zone 5 (Ultras)", 200.0);

    private final String display;
    private final double defaultPrice;

    ZoneType(String display, double defaultPrice) { this.display = display; this.defaultPrice = defaultPrice; }

    @Override
    public String toString() { return display; }

    public double getDefaultPrice() { return defaultPrice; }

    public static ZoneType fromString(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        switch (t) {
            case "vip": return VIP;
            case "zone1": case "zone 1": case "1": return ZONE1;
            case "zone2": case "zone 2": case "2": return ZONE2;
          
            default:
                for (ZoneType z : values()) {
                    if (z.display.toLowerCase().startsWith(t) || z.name().toLowerCase().startsWith(t)) return z;
                }
                return null;
        }
    }
}
