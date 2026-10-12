package xdevs.core.examples.dynamic.bench;

/** DEVStone HI topology (high input couplings: chain of atomics at every level). */
public class BenchHI extends BenchStone {

    public BenchHI(String prefix, int width, int depth, double preparationTime, int work) {
        super(prefix + (depth - 1));
        if (depth == 1) {
            BenchAtomic atomic = new BenchAtomic("A1_" + name, preparationTime, work);
            super.addComponent(atomic);
            super.addCoupling(iIn, atomic.iIn);
            super.addCoupling(atomic.oOut, oOut);
        } else {
            BenchHI coupled = new BenchHI(prefix, width, depth - 1, preparationTime, work);
            super.addComponent(coupled);
            super.addCoupling(iIn, coupled.iIn);
            super.addCoupling(coupled.oOut, oOut);
            BenchAtomic prev = null;
            for (int i = 0; i < (width - 1); ++i) {
                BenchAtomic atomic = new BenchAtomic("A" + (i + 1) + "_" + name, preparationTime, work);
                super.addComponent(atomic);
                super.addCoupling(iIn, atomic.iIn);
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
