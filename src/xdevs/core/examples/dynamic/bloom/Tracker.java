package xdevs.core.examples.dynamic.bloom;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * Inference service instance dedicated to one bloom (one tracker per detected bloom). It filters
 * the reports of its bloom and publishes an estimate every two hours.
 */
public class Tracker extends Atomic {

    public Port<Events.Report> iReport = new Port<>("iReport");
    public Port<Events.Estimate> oEstimate = new Port<>("oEstimate");

    public final int bloomId;
    private int samples = 0;
    private double estimate = 0.0;

    public Tracker(int bloomId) {
        super("tracker-" + bloomId);
        super.addInPort(iReport);
        super.addOutPort(oEstimate);
        this.bloomId = bloomId;
    }

    @Override
    public void initialize() { super.holdIn("tracking", 2.0); }

    @Override
    public void exit() {}

    @Override
    public void deltint() { super.holdIn("tracking", 2.0); }

    @Override
    public void deltext(double e) {
        super.resume(e);
        for (Events.Report r : iReport.getValues()) {
            if (r.id == bloomId) { samples++; estimate = 0.7 * estimate + 0.3 * r.density; }
        }
    }

    @Override
    public void lambda() { oEstimate.addValue(new Events.Estimate(bloomId, estimate, samples)); }
}
