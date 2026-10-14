package xdevs.core.examples.dynamic.bloom;

import java.util.ArrayList;
import java.util.List;

import xdevs.core.modeling.Component;
import xdevs.core.modeling.Coupled;
import xdevs.core.modeling.Port;

/**
 * Water body with a variable number of blooms (DS-coupled model).
 *
 * Structural transition function (reads only the sequential states of its children plus the
 * executive variable {@link #lastBirthSeen}):
 *   R4: for every birth emitted by the spawner that has not been materialised yet, add a Bloom
 *       atomic model and couple its report port to the water body's output port;
 *   R5: remove every Bloom in phase "faded".
 * Stability: (a) nothing depends on elapsed times; (b) after applying R4/R5 the pending births are
 * none and the faded blooms are gone, so a second evaluation changes nothing; (c) initially there are
 * no births and no blooms.
 */
public class WaterBody extends Coupled {

    public Port<Events.Report> oReports = new Port<>("oReports");

    private final BloomSpawner spawner;
    private final double nutrients;
    private int lastBirthSeen = 0;                 // executive variable
    public final List<Bloom> blooms = new ArrayList<>();
    public int bornTotal = 0, fadedTotal = 0;

    public WaterBody(String name, double[] birthHours, double nutrients) {
        super(name);
        super.addOutPort(oReports);
        this.nutrients = nutrients;
        spawner = new BloomSpawner("spawner", birthHours);
        super.addComponent(spawner);
    }

    @Override
    public boolean structuralTransition() {
        boolean changed = false;
        // R5: remove faded blooms
        for (int i = blooms.size() - 1; i >= 0; i--) {
            Bloom b = blooms.get(i);
            if (b.phaseIs(Bloom.PHASE_FADED)) {
                super.removeComponent(b);
                blooms.remove(i);
                fadedTotal++;
                changed = true;
            }
        }
        // R4: materialise new births
        for (Events.Birth birth : spawner.births) {
            if (birth.id <= lastBirthSeen) continue;
            Bloom b = new Bloom(birth.id, birth.lon, birth.lat, birth.density, spawnerClockHint(birth), nutrients);
            super.addComponent(b);
            super.addCoupling(b.oReport, oReports);
            blooms.add(b);
            lastBirthSeen = birth.id;
            bornTotal++;
            changed = true;
        }
        return changed;
    }

    private double spawnerClockHint(Events.Birth birth) {
        return birth.id * 7.0; // deterministic start hour per bloom (keeps blooms out of phase)
    }

    public int activeBlooms() {
        int n = 0;
        for (Component c : getComponents()) if (c instanceof Bloom && !((Bloom) c).phaseIs(Bloom.PHASE_FADED)) n++;
        return n;
    }
}
