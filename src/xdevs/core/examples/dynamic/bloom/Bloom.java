package xdevs.core.examples.dynamic.bloom;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * A harmful algal bloom as an atomic model. Simplified version of the base model of the
 * DEVS-BLOOM inference service: the density grows with sunlight and nutrients and decays
 * otherwise; the bloom drifts with the water. It reports every hour and, once its density
 * falls below a threshold, enters the phase "faded": the water body then removes it.
 */
public class Bloom extends Atomic {

    public static final String PHASE_ALIVE = "alive";
    public static final String PHASE_FADED = "faded";

    public Port<Events.Report> oReport = new Port<>("oReport");

    public final int id;
    private double density, lon, lat;
    private double hour;          // hour of day (0-24), part of the state
    private double nutrients;     // local nutrient stock, consumed by the bloom (part of the state)
    private final double kGrowth, kDecay, kConsume;

    public Bloom(int id, double lon, double lat, double density, double startHour, double nutrients) {
        super("bloom-" + id);
        super.addOutPort(oReport);
        this.id = id; this.lon = lon; this.lat = lat; this.density = density;
        this.hour = startHour % 24.0; this.nutrients = nutrients;
        this.kGrowth = 0.25; this.kDecay = 0.08; this.kConsume = 0.15;
    }

    @Override
    public void initialize() { super.holdIn(PHASE_ALIVE, 1.0); }

    @Override
    public void exit() {}

    @Override
    public void deltint() {
        if (phaseIs(PHASE_FADED)) { super.passivate(PHASE_FADED); return; }
        hour = (hour + 1.0) % 24.0;
        double sun = Math.max(0.0, Math.sin(Math.PI * (hour - 6.0) / 12.0)); // daylight 6h-18h
        double growth = kGrowth * sun * nutrients * density;
        density = density + growth - kDecay * density;
        nutrients = Math.max(0.0, nutrients - kConsume * growth);          // the bloom exhausts its patch
        density = Math.min(density, 10.0);
        lon += 0.0005; lat -= 0.0002;                        // drift with the current
        if (density < 0.05) { super.holdIn(PHASE_FADED, 1.0); }   // last report, then passive
        else super.holdIn(PHASE_ALIVE, 1.0);
    }

    @Override
    public void deltext(double e) { super.resume(e); }

    @Override
    public void lambda() {
        oReport.addValue(new Events.Report(id, density, lon, lat, phaseIs(PHASE_FADED)));
    }

    public double getDensity() { return density; }
}
