package xdevs.core.examples.dynamic.bloom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xdevs.core.modeling.Coupled;
import xdevs.core.modeling.Port;

/**
 * Fog layer (ground control station) with a variable number of trackers and USVs (DS-coupled model).
 *
 * Structural transition function (reads the state of the GCS, of the trackers and of the USVs,
 * plus the executive variables below):
 *   R1: a bloom whose last reported density is above DETECT and has no tracker gets a Tracker;
 *   R2: a tracker whose bloom's last reported density is below RELEASE (or whose bloom faded) is removed;
 *   R3: USVs in phase "depleted" are removed; while there are fewer patrolling USVs than trackers
 *       (capped by the fleet size and the maximum simultaneous USVs), a new USV is launched.
 * DETECT > RELEASE is the hysteresis band that prevents the structural oscillation observed in the
 * factory case study when a single threshold is used in both directions.
 * Stability: (a) only sequential states and executive variables are read; (b) after applying
 * R1-R3 no rule fires again on the same states; (c) initially there are no blooms, trackers or USVs.
 */
public class Fog extends Coupled {

    public static final double DETECT = 0.5, RELEASE = 0.1;

    public Port<Events.Report> iReports = new Port<>("iReports");
    public Port<Events.Estimate> oEstimates = new Port<>("oEstimates");

    public final GCS gcs;
    private final List<Tracker> trackers = new ArrayList<>();
    private final List<USV> usvs = new ArrayList<>();
    private final int fleetSize, maxSimultaneous;
    private final double usvDrain;
    private int launched = 0;                       // executive variable
    public int trackersCreated = 0, trackersRemoved = 0, usvsRemoved = 0;

    public Fog(String name, int fleetSize, int maxSimultaneous, double usvDrain) {
        super(name);
        super.addInPort(iReports);
        super.addOutPort(oEstimates);
        this.fleetSize = fleetSize; this.maxSimultaneous = maxSimultaneous; this.usvDrain = usvDrain;
        gcs = new GCS("gcs");
        super.addComponent(gcs);
        super.addCoupling(iReports, gcs.iReport);
    }

    private Tracker trackerOf(int bloomId) {
        for (Tracker t : trackers) if (t.bloomId == bloomId) return t;
        return null;
    }

    @Override
    public boolean structuralTransition() {
        boolean changed = false;
        // R2: release trackers of blooms that faded or fell below RELEASE
        for (int i = trackers.size() - 1; i >= 0; i--) {
            Tracker t = trackers.get(i);
            Double d = gcs.lastDensity.get(t.bloomId);
            boolean faded = Boolean.TRUE.equals(gcs.faded.get(t.bloomId));
            if (d == null || faded || d < RELEASE) {
                super.removeComponent(t);
                trackers.remove(i);
                trackersRemoved++;
                changed = true;
            }
        }
        // R1: detect blooms above DETECT without a tracker
        for (Map.Entry<Integer, Double> e : gcs.lastDensity.entrySet()) {
            if (e.getValue() > DETECT && !Boolean.TRUE.equals(gcs.faded.get(e.getKey())) && trackerOf(e.getKey()) == null) {
                Tracker t = new Tracker(e.getKey());
                super.addComponent(t);
                super.addCoupling(iReports, t.iReport);
                super.addCoupling(t.oEstimate, oEstimates);
                super.addCoupling(t.oEstimate, gcs.iEstimate);
                trackers.add(t);
                trackersCreated++;
                changed = true;
            }
        }
        // R3: fleet management
        for (int i = usvs.size() - 1; i >= 0; i--) {
            USV u = usvs.get(i);
            if (u.phaseIs(USV.PHASE_DEPLETED)) {
                super.removeComponent(u);
                usvs.remove(i);
                usvsRemoved++;
                changed = true;
            }
        }
        int want = Math.min(trackers.size(), maxSimultaneous);
        while (usvs.size() < want && launched < fleetSize) {
            USV u = new USV("usv-" + (++launched), usvDrain);
            super.addComponent(u);
            super.addCoupling(u.oTelemetry, gcs.iTelemetry);
            usvs.add(u);
            changed = true;
        }
        return changed;
    }

    public int activeTrackers() { return trackers.size(); }
    public int activeUsvs() { return usvs.size(); }
    public int launched() { return launched; }
}
