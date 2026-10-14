package xdevs.core.examples.dynamic.bloom;

import java.util.ArrayList;
import java.util.List;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * Deterministic source of bloom births (a stand-in for the EEMS lake data of DEVS-BLOOM).
 * Births happen at the configured hours. The births emitted so far are part of the sequential
 * state ({@link #births}), which is what the water body's structural transition function reads.
 */
public class BloomSpawner extends Atomic {

    public Port<Events.Birth> oBirth = new Port<>("oBirth");

    private final double[] hours;
    private int next = 0;
    private double clock = 0.0;
    /** Births emitted so far (sequential state, read by the water body). */
    public final List<Events.Birth> births = new ArrayList<>();

    public BloomSpawner(String name, double[] hours) {
        super(name);
        super.addOutPort(oBirth);
        this.hours = hours;
    }

    @Override
    public void initialize() {
        next = 0; clock = 0.0; births.clear();
        schedule();
    }

    private void schedule() {
        if (next < hours.length) super.holdIn("waiting", hours[next] - clock);
        else super.passivate();
    }

    @Override
    public void exit() {}

    /** Birth number k (deterministic): incubator area near the channel mouth, as in the DEVS-BLOOM use case. */
    private Events.Birth makeBirth(int k) {
        double lon = -122.215 + 0.01 * (k % 3), lat = 47.505 + 0.005 * (k % 2);
        return new Events.Birth(k + 1, lon, lat, 0.6);
    }

    @Override
    public void deltint() {
        clock = hours[next];
        births.add(makeBirth(next));
        next++;
        schedule();
    }

    @Override
    public void deltext(double e) { super.resume(e); }

    @Override
    public void lambda() {
        if (next < hours.length) oBirth.addValue(makeBirth(next)); // informational (the water body reads the state)
    }
}
