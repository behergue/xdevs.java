package xdevs.core.verification.demo;

import java.util.function.Supplier;
import java.util.logging.Level;

import xdevs.core.examples.dynamic.Factory;
import xdevs.core.examples.dynamic.bench.BenchGenerator;
import xdevs.core.examples.dynamic.bench.BenchHO;
import xdevs.core.examples.dynamic.bench.DynHO;
import xdevs.core.modeling.Coupled;
import xdevs.core.util.DevsLogger;
import xdevs.core.verification.Invariant;
import xdevs.core.verification.VerificationReport;
import xdevs.core.verification.Verifier;

/**
 * Runs the offline verifier on: the Factory case study, the dynamic HO benchmark, a static HO model
 * with closed-form counts, and two deliberately broken models.
 */
public class VerifyDemo {

    static Supplier<Coupled> factory(int maxMachines) {
        return () -> new Factory("factory", 1.0, 3.0, 200.0, maxMachines);
    }

    static Supplier<Coupled> unstableFactory() {
        return () -> new UnstableFactory("factory", 1.0, 3.0, 50.0, 4);
    }

    static Supplier<Coupled> dynHO(int w, int d, int n, int P, int s) {
        return () -> {
            Coupled fw = new Coupled("Framework");
            BenchGenerator gen = new BenchGenerator("Generator", 1.0, n);
            DynHO stone = new DynHO("C", w, d, 0.0, 0, P, s);
            fw.addComponent(gen);
            fw.addComponent(stone);
            fw.addCoupling(gen.oOut, stone.iIn);
            fw.addCoupling(gen.oOut, stone.iInAux);
            return fw;
        };
    }

    static Supplier<Coupled> staticHO(int w, int d, int n) {
        return () -> {
            Coupled fw = new Coupled("Framework");
            BenchGenerator gen = new BenchGenerator("Generator", 1.0, n);
            BenchHO stone = new BenchHO("C", w, d, 0.0, 0);
            fw.addComponent(gen);
            fw.addComponent(stone);
            fw.addCoupling(gen.oOut, stone.iIn);
            fw.addCoupling(gen.oOut, stone.iInAux);
            return fw;
        };
    }

    /** An active HO model next to idle dynamic subsystems: exercises the quiet-cycle check. */
    static Supplier<Coupled> withIdle(Supplier<Coupled> active, Supplier<Coupled> idle) {
        return () -> {
            Coupled fw = active.get();
            fw.addComponent(idle.get());
            return fw;
        };
    }

    static void run(String title, Supplier<Coupled> model, Long expectedInts, Long expectedExts) {
        System.out.println("==== " + title);
        Verifier.Result r = Verifier.verify(model, Long.MAX_VALUE);
        Verifier.verifyEagerLazyEquivalence(model, Long.MAX_VALUE, r.report);
        if (expectedInts != null) {
            Verifier.verifyExpectedCounts(r.digest, expectedInts, expectedExts, r.report);
        }
        System.out.println("trace: " + r.digest);
        System.out.print(r.report);
        System.out.println();
    }

    public static void main(String[] args) {
        DevsLogger.setup(Level.SEVERE);
        run("Factory, maxMachines=3 (stable, no oscillation)", factory(3), null, null);
        run("Factory, maxMachines=4 (stable, oscillating)", factory(4), null, null);
        int w = 6, d = 4, n = 40;
        long theory = (long) n * (((long) (w * w - w) / 2) * (d - 1) + 1) + n; // DEVStone HO + the generator's own internal transitions
        run("Static HO 6x4, n=40 (closed-form counts)", staticHO(w, d, n), theory, theory - n);
        run("Dynamic HO 6x4, n=40, churn every 5 events of 2 atomics", dynHO(w, d, n, 5, 2), null, null);
        run("Static HO 6x4 + an idle DynHO copy (quiet cycles, stable)", withIdle(staticHO(w, d, n), () -> new DynHO("I", w, d, 0.0, 0, 5, 2)), null, null);
        run("UnstableFactory (violates stability on purpose)", unstableFactory(), null, null);
        run("Static HO 6x4 + QuietToggler (changes structure in quiet cycles)", withIdle(staticHO(w, d, n), () -> new QuietToggler("toggler")), null, null);
        run("BadTimingAtomic (negative ta, output outside lambda)", BadTimingAtomic::example, null, null);
        System.out.println("Invariants:");
        for (Invariant i : Invariant.values()) {
            System.out.println("  " + i + ": " + i.description);
        }
    }
}
