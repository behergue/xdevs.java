package xdevs.core.examples.dynamic.bloom;

/** Event types exchanged in the dynamic DEVS-BLOOM prototype. Time unit: hours. */
public final class Events {
    private Events() {}

    /** A bloom is born at a position (emitted by the spawner). */
    public static final class Birth {
        public final int id; public final double lon, lat, density;
        public Birth(int id, double lon, double lat, double density) { this.id = id; this.lon = lon; this.lat = lat; this.density = density; }
        @Override public String toString() { return "Birth#" + id; }
    }

    /** Density report of a bloom (emitted hourly by each Bloom atomic model). */
    public static final class Report {
        public final int id; public final double density, lon, lat; public final boolean faded;
        public Report(int id, double density, double lon, double lat, boolean faded) { this.id = id; this.density = density; this.lon = lon; this.lat = lat; this.faded = faded; }
        @Override public String toString() { return String.format("Report#%d(%.2f)", id, density); }
    }

    /** Estimate produced by a tracker (position and size of the bloom it follows). */
    public static final class Estimate {
        public final int bloomId; public final double density; public final int samples;
        public Estimate(int bloomId, double density, int samples) { this.bloomId = bloomId; this.density = density; this.samples = samples; }
        @Override public String toString() { return String.format("Estimate#%d(%.2f,n=%d)", bloomId, density, samples); }
    }

    /** Telemetry of a USV (battery level). */
    public static final class Telemetry {
        public final String usv; public final double battery;
        public Telemetry(String usv, double battery) { this.usv = usv; this.battery = battery; }
        @Override public String toString() { return String.format("Telemetry(%s,%.0f%%)", usv, battery); }
    }
}
