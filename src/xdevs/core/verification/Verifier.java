package xdevs.core.verification;

import java.util.function.Supplier;

import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;

/**
 * Entry point of the offline verification: runs a model under the verifying simulators (I1-I7),
 * optionally compares the eager and lazy policies (I8) and the transition counts with a closed form (I9).
 *
 * The model is supplied through a factory so that independent, fresh instances can be built for each run.
 */
public class Verifier {

    public static class Result {
        public final VerificationReport report;
        public final TraceDigest digest;
        Result(VerificationReport report, TraceDigest digest) {
            this.report = report;
            this.digest = digest;
        }
    }

    /** Runs the model to completion under the verifying coordinator and returns the report. */
    public static Result verify(Supplier<Coupled> factory, long maxIterations) {
        VerificationReport report = new VerificationReport();
        TraceDigest digest = new TraceDigest();
        Coupled model = factory.get();
        VerifyingCoordinator c = new VerifyingCoordinator(model, report, digest);
        c.initialize();
        c.simulate(maxIterations);
        c.exit();
        return new Result(report, digest);
    }

    /** I8: simulates the model with the eager and the lazy policies and compares their traces. */
    public static boolean verifyEagerLazyEquivalence(Supplier<Coupled> factory, long maxIterations, VerificationReport report) {
        TraceDigest eager = runPlain(factory.get(), false, maxIterations);
        TraceDigest lazy = runPlain(factory.get(), true, maxIterations);
        report.checked(Invariant.EAGER_LAZY_EQUIVALENCE);
        boolean same = eager.sameAs(lazy);
        if (!same) {
            report.violated(Invariant.EAGER_LAZY_EQUIVALENCE, Double.NaN, "<model>", "eager: " + eager + " | lazy: " + lazy);
        }
        return same;
    }

    /**
     * I9: compares the observed transition counts with expected values. A confluent transition counts
     * both as an internal and as an external one, which is the convention of the DEVStone formulas.
     */
    public static void verifyExpectedCounts(TraceDigest digest, long expectedInts, long expectedExts, VerificationReport report) {
        report.checked(Invariant.EXPECTED_COUNTS);
        long ints = digest.deltInts + digest.deltCons;
        long exts = digest.deltExts + digest.deltCons;
        if (ints != expectedInts || exts != expectedExts) {
            report.violated(Invariant.EXPECTED_COUNTS, Double.NaN, "<model>",
                    String.format("observed int+con=%d ext+con=%d, expected int=%d ext=%d", ints, exts, expectedInts, expectedExts));
        }
    }

    /** Runs a model with a (non-verifying) dynamic coordinator wrapped only to collect the digest. */
    private static TraceDigest runPlain(Coupled model, boolean lazy, long maxIterations) {
        VerificationReport dummy = new VerificationReport();
        TraceDigest digest = new TraceDigest();
        CoordinatorDynamic c = new DigestCoordinator(model, lazy, dummy, digest);
        c.initialize();
        c.simulate(maxIterations);
        c.exit();
        return digest;
    }

    /** Coordinator with the requested policy whose atomic simulators record the digest but check nothing. */
    static class DigestCoordinator extends CoordinatorDynamic {
        private final VerificationReport dummy;
        private final TraceDigest digest;

        DigestCoordinator(Coupled model, boolean lazy, VerificationReport dummy, TraceDigest digest) {
            super(model, lazy, Reconcile.MAP);
            this.dummy = dummy;
            this.digest = digest;
        }

        DigestCoordinator(xdevs.core.simulation.SimulationClock clock, Coupled model, boolean lazy, VerificationReport dummy, TraceDigest digest) {
            super(clock, model, lazy, Reconcile.MAP);
            this.dummy = dummy;
            this.digest = digest;
        }

        @Override
        protected xdevs.core.simulation.AbstractSimulator newSimulator(xdevs.core.modeling.Component component) {
            if (component instanceof Coupled) {
                return new DigestCoordinator(clock, (Coupled) component, lazy, dummy, digest);
            }
            return new VerifyingSimulator(clock, (xdevs.core.modeling.Atomic) component, dummy, digest);
        }

        @Override
        public boolean structuralTransition() {
            VerifyingCoordinator.Snapshot before = VerifyingCoordinator.Snapshot.of(model);
            boolean changed = super.structuralTransition();
            if (changed) {
                VerifyingCoordinator.Snapshot after = VerifyingCoordinator.Snapshot.of(model);
                digest.structuralChanges++;
                digest.componentsAdded += after.added(before);
                digest.componentsRemoved += before.added(after);
                digest.event(clock.getTime(), model.getName(), "str");
            }
            return changed;
        }
    }
}
