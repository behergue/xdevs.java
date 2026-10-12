package xdevs.core.examples.dynamic.bench;

/** DEVStone LI topology (low level of interconnections). */
public class BenchLI extends BenchStone {

    public BenchLI(String prefix, int width, int depth, double preparationTime, int work) {
        super(prefix + (depth - 1));
        if (depth == 1) {
            BenchAtomic atomic = new BenchAtomic("A1_" + name, preparationTime, work);
            super.addComponent(atomic);
            super.addCoupling(iIn, atomic.iIn);
            super.addCoupling(atomic.oOut, oOut);
        } else {
            BenchLI coupled = new BenchLI(prefix, width, depth - 1, preparationTime, work);
            super.addComponent(coupled);
            super.addCoupling(iIn, coupled.iIn);
            super.addCoupling(coupled.oOut, oOut);
            for (int i = 0; i < (width - 1); ++i) {
                BenchAtomic atomic = new BenchAtomic("A" + (i + 1) + "_" + name, preparationTime, work);
                super.addComponent(atomic);
                super.addCoupling(iIn, atomic.iIn);
            }
        }
    }

    @Override
    public long numDeltExtsInTheory(int maxEvents, int width, int depth) {
        return (long) maxEvents * ((width - 1) * (depth - 1) + 1);
    }

    @Override
    public int numAtomicsInTheory(int width, int depth) {
        return (width - 1) * (depth - 1) + 1;
    }
}
