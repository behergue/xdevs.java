package xdevs.core.examples.dynamic.bench;

import xdevs.core.modeling.Port;

/** DEVStone HO topology (high input and output couplings). */
public class BenchHO extends BenchStone {

    public Port<Integer> iInAux = new Port<>("inAux");
    public Port<Integer> oOutAux = new Port<>("outAux");

    public BenchHO(String name) {
        super(name);
        super.addInPort(iInAux);
        super.addOutPort(oOutAux);
    }

    public BenchHO(String prefix, int width, int depth, double preparationTime, int work) {
        this(prefix + (depth - 1));
        if (depth == 1) {
            BenchAtomic atomic = new BenchAtomic("A1_" + name, preparationTime, work);
            super.addComponent(atomic);
            super.addCoupling(iIn, atomic.iIn);
            super.addCoupling(atomic.oOut, oOut);
        } else {
            BenchHO coupled = new BenchHO(prefix, width, depth - 1, preparationTime, work);
            super.addComponent(coupled);
            super.addCoupling(iIn, coupled.iIn);
            super.addCoupling(iIn, coupled.iInAux);
            super.addCoupling(coupled.oOut, oOut);
            BenchAtomic prev = null;
            for (int i = 0; i < (width - 1); ++i) {
                BenchAtomic atomic = new BenchAtomic("A" + (i + 1) + "_" + name, preparationTime, work);
                super.addComponent(atomic);
                super.addCoupling(iInAux, atomic.iIn);
                super.addCoupling(atomic.oOut, oOutAux);
                if (prev != null) {
                    super.addCoupling(prev.oOut, atomic.iIn);
                }
                prev = atomic;
            }
        }
    }

    @Override
    public long numDeltExtsInTheory(int maxEvents, int width, int depth) {
        return (long) maxEvents * (((long) (width * width - width) / 2) * (depth - 1) + 1);
    }

    @Override
    public int numAtomicsInTheory(int width, int depth) {
        return (width - 1) * (depth - 1) + 1;
    }
}
