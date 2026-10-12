package xdevs.core.examples.dynamic.bench;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * Event generator (as DevStoneGenerator): emits the integers 1..maxEvents with a given period.
 */
public class BenchGenerator extends Atomic {

    public Port<Integer> oOut = new Port<>("out");
    protected final double period;
    protected final int maxEvents;
    protected int counter = 1;

    public BenchGenerator(String name, double period, int maxEvents) {
        super(name);
        super.addOutPort(oOut);
        this.period = period;
        this.maxEvents = maxEvents;
    }

    @Override
    public void initialize() {
        counter = 1;
        super.holdIn("active", 0.0);
    }

    @Override
    public void exit() {
    }

    @Override
    public void deltint() {
        counter++;
        if (counter > maxEvents) {
            super.passivate();
        } else {
            super.holdIn("active", period);
        }
    }

    @Override
    public void deltext(double e) {
        super.resume(e);
        super.passivate();
    }

    @Override
    public void lambda() {
        oOut.addValue(counter);
    }
}
