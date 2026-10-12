package xdevs.core.examples.dynamic.bench;

import xdevs.core.modeling.Component;
import xdevs.core.modeling.Coupled;
import xdevs.core.modeling.Port;

/**
 * Base class of the DEVStone-like benchmark models (same topologies as xDEVS DEVStone LI/HI/HO).
 */
public abstract class BenchStone extends Coupled {

    public Port<Integer> iIn = new Port<>("in");
    public Port<Integer> oOut = new Port<>("out");

    public BenchStone(String name) {
        super(name);
        super.addInPort(iIn);
        super.addOutPort(oOut);
    }

    public long numDeltExtsInPractice() {
        long n = 0;
        for (Component c : super.getComponents()) {
            if (c instanceof BenchAtomic) n += ((BenchAtomic) c).numDeltExts;
            else if (c instanceof BenchStone) n += ((BenchStone) c).numDeltExtsInPractice();
        }
        return n;
    }

    public long numDeltIntsInPractice() {
        long n = 0;
        for (Component c : super.getComponents()) {
            if (c instanceof BenchAtomic) n += ((BenchAtomic) c).numDeltInts;
            else if (c instanceof BenchStone) n += ((BenchStone) c).numDeltIntsInPractice();
        }
        return n;
    }

    public long numEventsInPractice() {
        long n = 0;
        for (Component c : super.getComponents()) {
            if (c instanceof BenchAtomic) n += ((BenchAtomic) c).numOfEvents;
            else if (c instanceof BenchStone) n += ((BenchStone) c).numEventsInPractice();
        }
        return n;
    }

    public abstract long numDeltExtsInTheory(int maxEvents, int width, int depth);

    public abstract int numAtomicsInTheory(int width, int depth);
}
