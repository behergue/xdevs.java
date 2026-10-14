package xdevs.core.examples.dynamic.bloom;

import java.util.Collection;
import java.util.TreeMap;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * Ground Control Station of the fog layer: stores the last report of every bloom and the last
 * telemetry of every USV (its database). It is passive: its state is what the fog's structural
 * transition function reads to decide which trackers and USVs must exist.
 */
public class GCS extends Atomic {

    public Port<Events.Report> iReport = new Port<>("iReport");
    public Port<Events.Telemetry> iTelemetry = new Port<>("iTelemetry");
    public Port<Events.Estimate> iEstimate = new Port<>("iEstimate");

    /** Last density reported per bloom id (sequential state). */
    public final TreeMap<Integer, Double> lastDensity = new TreeMap<>();
    public final TreeMap<Integer, Boolean> faded = new TreeMap<>();
    public final TreeMap<String, Double> battery = new TreeMap<>();
    public long reports = 0, estimates = 0, telemetries = 0;

    public GCS(String name) {
        super(name);
        super.addInPort(iReport);
        super.addInPort(iTelemetry);
        super.addInPort(iEstimate);
    }

    @Override
    public void initialize() { super.passivate(); }

    @Override
    public void exit() {}

    @Override
    public void deltint() { super.passivate(); }

    @Override
    public void deltext(double e) {
        super.resume(e);
        Collection<Events.Report> rs = iReport.getValues();
        for (Events.Report r : rs) {
            lastDensity.put(r.id, r.density);
            faded.put(r.id, r.faded);
            reports++;
        }
        for (Events.Telemetry t : iTelemetry.getValues()) { battery.put(t.usv, t.battery); telemetries++; }
        estimates += iEstimate.getValues().size();
        super.passivate();
    }

    @Override
    public void lambda() {}
}
