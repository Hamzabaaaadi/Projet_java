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
            case "zone3": case "zone 3": case "3": return ZONE3;
            case "zone4": case "zone 4": case "4": return ZONE4;
            case "zone5": case "zone 5": case "5": case "ultras": return ZONE5_ULTRAS;
            default:
                for (ZoneType z : values()) {
                    if (z.display.toLowerCase().startsWith(t) || z.name().toLowerCase().startsWith(t)) return z;
                }
                return null;
        }
    }
}
