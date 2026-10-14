package xdevs.core.verification;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Component;
import xdevs.core.modeling.Coupled;
import xdevs.core.modeling.Coupling;
import xdevs.core.modeling.Port;
import xdevs.core.simulation.AbstractSimulator;
import xdevs.core.simulation.SimulationClock;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;

/**
 * Dynamic coordinator that checks the dynamic-structure invariants I5-I7 around the structural phase
 * and builds verifying simulators for its children. It always runs the eager policy internally so that
 * delta_str can be observed in quiet cycles (I7c); the lazy policy is verified separately by trace
 * equivalence (I8) in {@link Verifier}.
 */
public class VerifyingCoordinator extends CoordinatorDynamic {

    private final VerificationReport report;
    private final TraceDigest digest;
    private final int maxZeroTimeCycles;
    private double lastCycleTime = Double.NaN;
    private int zeroTimeCycles = 0;

    public VerifyingCoordinator(SimulationClock clock, Coupled model, VerificationReport report, TraceDigest digest, int maxZeroTimeCycles) {
        super(clock, model, false, Reconcile.MAP);
        this.report = report;
        this.digest = digest;
        this.maxZeroTimeCycles = maxZeroTimeCycles;
    }

    public VerifyingCoordinator(Coupled model, VerificationReport report, TraceDigest digest) {
        this(new SimulationClock(), model, report, digest, 100_000);
    }

    @Override
    protected AbstractSimulator newSimulator(Component component) {
        if (component instanceof Coupled) {
            return new VerifyingCoordinator(clock, (Coupled) component, report, digest, maxZeroTimeCycles);
        }
        return new VerifyingSimulator(clock, (Atomic) component, report, digest);
    }

    @Override
    public void initialize() {
        super.initialize();
        // I7a: the initial structure is a fixed point of delta_str
        report.checked(Invariant.STABILITY_INITIAL);
        Snapshot before = Snapshot.of(model);
        boolean changed = model.structuralTransition();
        if (changed || !before.sameAs(Snapshot.of(model))) {
            report.violated(Invariant.STABILITY_INITIAL, clock.getTime(), model.getName(), "delta_str changed the initial structure (returned " + changed + ")");
            super.structuralTransition(); // keep simulators consistent to continue
        }
        checkStructure(clock.getTime(), "after initialize()");
    }

    @Override
    public void deltfcn() {
        double t = clock.getTime();
        // I5: bounded number of cycles at the same instant (only counted at the root)
        if (model.getParent() == null) {
            report.checked(Invariant.LEGITIMACY_BOUNDED);
            if (t == lastCycleTime) {
                zeroTimeCycles++;
                if (zeroTimeCycles == maxZeroTimeCycles) {
                    report.violated(Invariant.LEGITIMACY_BOUNDED, t, model.getName(), maxZeroTimeCycles + " consecutive cycles at t=" + t + " (possible illegitimate model)");
                }
            } else {
                zeroTimeCycles = 0;
                lastCycleTime = t;
            }
        }
        super.deltfcn();
    }

    @Override
    public boolean structuralTransition() {
        double t = clock.getTime();
        boolean quiet = !childTransitioned;
        Snapshot before = Snapshot.of(model);
        boolean changed = super.structuralTransition();
        Snapshot after = Snapshot.of(model);
        boolean reallyChanged = !before.sameAs(after);
        if (quiet) {
            // I7c: in a quiet cycle delta_str must be the identity
            report.checked(Invariant.STABILITY_QUIET_CYCLE);
            if (changed || reallyChanged) {
                report.violated(Invariant.STABILITY_QUIET_CYCLE, t, model.getName(), "structure changed in a cycle with no child transition (returned " + changed + ")");
            }
        }
        if (changed != reallyChanged) {
            // the API contract: the return value must reflect whether the structure changed
            report.checked(Invariant.STRUCTURE_WELL_FORMED);
            report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "structuralTransition() returned " + changed + " but the structure " + (reallyChanged ? "changed" : "did not change"));
        }
        if (changed) {
            digest.structuralChanges++;
            digest.componentsAdded += after.added(before);
            digest.componentsRemoved += before.added(after);
            digest.event(t, model.getName(), "str");
            // I7b: idempotence right after a change
            report.checked(Invariant.STABILITY_IDEMPOTENT);
            Snapshot s1 = Snapshot.of(model);
            boolean again = model.structuralTransition();
            if (again || !s1.sameAs(Snapshot.of(model))) {
                report.violated(Invariant.STABILITY_IDEMPOTENT, t, model.getName(), "a second evaluation of delta_str changed the structure again (returned " + again + ")");
                super.structuralTransition(); // reconcile to continue
            }
            checkStructure(t, "after structural change");
        }
        return changed;
    }

    /** I6: couplings refer to ports of current components (or of the coupled model itself), no self-coupling, simulators == components. */
    private void checkStructure(double t, String when) {
        report.checked(Invariant.STRUCTURE_WELL_FORMED);
        Map<Component, Boolean> current = new IdentityHashMap<>();
        for (Component c : model.getComponents()) current.put(c, Boolean.TRUE);
        for (LinkedList<Coupling<?>> list : java.util.List.of(model.getEIC(), model.getIC(), model.getEOC())) {
            for (Coupling<?> c : list) {
                Component from = c.getPortFrom().getParent();
                Component to = c.getPortTo().getParent();
                if (from == to) {
                    report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "self-coupling " + c + " " + when);
                }
                if (from != model && !current.containsKey(from)) {
                    report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "coupling from removed/unknown component " + from.getName() + " " + when);
                }
                if (to != model && !current.containsKey(to)) {
                    report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "coupling to removed/unknown component " + to.getName() + " " + when);
                }
            }
        }
        Map<Component, Boolean> simulated = new IdentityHashMap<>();
        for (AbstractSimulator s : simulators) simulated.put(s.getModel(), Boolean.TRUE);
        for (Component c : model.getComponents()) {
            if (!simulated.containsKey(c)) {
                report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "component " + c.getName() + " has no simulator " + when);
            }
        }
        for (AbstractSimulator s : simulators) {
            if (!current.containsKey(s.getModel())) {
                report.violated(Invariant.STRUCTURE_WELL_FORMED, t, model.getName(), "simulator of removed component " + s.getModel().getName() + " " + when);
            }
        }
    }

    /** Identity-based snapshot of a coupled model's structure. */
    static final class Snapshot {
        final Map<Component, Boolean> components = new IdentityHashMap<>();
        final int eic, ic, eoc;

        private Snapshot(Coupled m) {
            for (Component c : m.getComponents()) components.put(c, Boolean.TRUE);
            eic = m.getEIC().size();
            ic = m.getIC().size();
            eoc = m.getEOC().size();
        }

        static Snapshot of(Coupled m) {
            return new Snapshot(m);
        }

        boolean sameAs(Snapshot o) {
            if (eic != o.eic || ic != o.ic || eoc != o.eoc || components.size() != o.components.size()) return false;
            for (Component c : components.keySet()) if (!o.components.containsKey(c)) return false;
            return true;
        }

        /** Number of components present here and absent in o. */
        int added(Snapshot o) {
            int n = 0;
            for (Component c : components.keySet()) if (!o.components.containsKey(c)) n++;
            return n;
        }
    }

    public static Collection<Port<?>> portsOf(Component c) {
        LinkedList<Port<?>> ps = new LinkedList<>(c.getInPorts());
        ps.addAll(c.getOutPorts());
        return ps;
    }
}
