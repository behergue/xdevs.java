package xdevs.core.examples.dynamic.bench;

import java.util.ArrayList;
import java.util.List;

import xdevs.core.modeling.Port;

/**
 * Dynamic structure variant of the HO DEVStone topology.
 *
 * Every level keeps a chain of atomic models. The structural transition function
 * removes the last {@code churnSize} atomics of the chain, or adds them back,
 * every time the first atomic of the chain has received an event whose
 * identifier is a multiple of {@code churnPeriod}.
 *
 * The decision depends only on the sequential state of the children (the
 * identifier of the last event received) and on an executive variable
 * ({@code lastChurnEvent}) that records the last event at which the structure
 * changed, so the function satisfies the stability assumption: it is
 * independent of the elapsed times, idempotent after a change, and the initial
 * structure is stable.
 */
public class DynHO extends BenchStone {

    public Port<Integer> iInAux = new Port<>("inAux");
    public Port<Integer> oOutAux = new Port<>("outAux");

    protected final List<BenchAtomic> chain = new ArrayList<>();
    protected final int fullWidth;      // number of atomics in the chain when complete
    protected final int churnPeriod;
    protected final int churnSize;
    protected final double preparationTime;
    protected final int work;
    protected int lastChurnEvent = 0;   // executive variable
    protected int nextId;               // monotonic counter for unique names

    public DynHO(String prefix, int width, int depth, double preparationTime, int work, int churnPeriod, int churnSize) {
        super(prefix + (depth - 1));
        super.addInPort(iInAux);
        super.addOutPort(oOutAux);
        this.preparationTime = preparationTime;
        this.work = work;
        this.churnPeriod = churnPeriod;
        this.churnSize = churnSize;
        if (depth == 1) {
            this.fullWidth = 1;
            BenchAtomic atomic = new BenchAtomic("A1_" + name, preparationTime, work);
            super.addComponent(atomic);
            super.addCoupling(iIn, atomic.iIn);
            super.addCoupling(atomic.oOut, oOut);
            this.nextId = 2;
        } else {
            this.fullWidth = width - 1;
            DynHO coupled = new DynHO(prefix, width, depth - 1, preparationTime, work, churnPeriod, churnSize);
            super.addComponent(coupled);
            super.addCoupling(iIn, coupled.iIn);
            super.addCoupling(iIn, coupled.iInAux);
            super.addCoupling(coupled.oOut, oOut);
            for (int i = 0; i < fullWidth; ++i) {
                appendAtomic();
            }
            this.nextId = fullWidth + 1;
        }
    }

    private void appendAtomic() {
        int id = chain.size() + 1;
        if (nextId > id) id = nextId;
        BenchAtomic atomic = new BenchAtomic("A" + id + "_" + name, preparationTime, work);
        nextId = id + 1;
        super.addComponent(atomic);
        super.addCoupling(iInAux, atomic.iIn);
        super.addCoupling(atomic.oOut, oOutAux);
        if (!chain.isEmpty()) {
            super.addCoupling(chain.get(chain.size() - 1).oOut, atomic.iIn);
        }
        chain.add(atomic);
    }

    @Override
    public boolean structuralTransition() {
        if (chain.isEmpty() || churnPeriod <= 0) {
            return false;
        }
        int ev = chain.get(0).lastEvent;
        if (ev == 0 || ev % churnPeriod != 0 || ev == lastChurnEvent) {
            return false;
        }
        lastChurnEvent = ev;
        if (chain.size() == fullWidth) {
            // Shrink: remove the last churnSize atomics (never below one atomic)
            int toRemove = Math.min(churnSize, chain.size() - 1);
            for (int i = 0; i < toRemove; i++) {
                BenchAtomic last = chain.remove(chain.size() - 1);
                super.removeComponent(last);
            }
            return toRemove > 0;
        } else {
            // Grow back to the full width
            while (chain.size() < fullWidth) {
                appendAtomic();
            }
            return true;
        }
    }

    public int chainSize() {
        return chain.size();
    }

    @Override
    public long numDeltExtsInTheory(int maxEvents, int width, int depth) {
        return -1; // depends on the structural trajectory
    }

    @Override
    public int numAtomicsInTheory(int width, int depth) {
        return (width - 1) * (depth - 1) + 1;
    }
}
