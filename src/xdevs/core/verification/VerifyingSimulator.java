package xdevs.core.verification;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;
import xdevs.core.simulation.SimulationClock;
import xdevs.core.simulation.Simulator;
import xdevs.core.util.Constants;

/**
 * Simulator of an atomic model that checks the PDEVS invariants I1-I4 around every phase.
 * The simulation itself is delegated to {@link Simulator}; the checks only observe.
 */
public class VerifyingSimulator extends Simulator {

    private final VerificationReport report;
    private final TraceDigest digest;
    private double lastEventTime = Double.NEGATIVE_INFINITY;

    public VerifyingSimulator(SimulationClock clock, Atomic model, VerificationReport report, TraceDigest digest) {
        super(clock, model);
        this.report = report;
        this.digest = digest;
    }

    private String name() {
        return model.getName();
    }

    private void checkTa(double t, String when) {
        report.checked(Invariant.TA_NON_NEGATIVE);
        double ta = model.ta();
        if (Double.isNaN(ta) || ta < 0) {
            report.violated(Invariant.TA_NON_NEGATIVE, t, name(), "ta = " + ta + " " + when + " (phase " + model.getPhase() + ")");
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        checkTa(clock.getTime(), "after initialize()");
    }

    @Override
    public void lambda() {
        double t = clock.getTime();
        boolean imminent = (t == tN);
        super.lambda();
        report.checked(Invariant.OUTPUT_ONLY_WHEN_IMMINENT);
        boolean hasOutput = false;
        for (Port<?> p : model.getOutPorts()) {
            if (!p.isEmpty()) {
                hasOutput = true;
                break;
            }
        }
        if (hasOutput && !imminent) {
            report.violated(Invariant.OUTPUT_ONLY_WHEN_IMMINENT, t, name(), "output produced while not imminent (tN=" + tN + ")");
        }
        if (hasOutput) {
            digest.outputs++;
            digest.event(t, name(), "out");
        }
    }

    @Override
    public void deltfcn() {
        double t = clock.getTime();
        boolean imminent = (t == tN);
        boolean input = !model.isInputEmpty();
        if (!imminent && !input) {
            super.deltfcn(); // no transition; nothing to check
            return;
        }
        // I4: time monotonic and within [tL, tN]
        report.checked(Invariant.TIME_MONOTONIC);
        if (t < lastEventTime || t < tL || t > tN) {
            report.violated(Invariant.TIME_MONOTONIC, t, name(), String.format("tL=%s t=%s tN=%s last=%s", tL, t, tN, lastEventTime));
        }
        // I3: elapsed time within the time advance of the current state
        if (input && !imminent) {
            report.checked(Invariant.ELAPSED_WITHIN_TA);
            double e = t - tL;
            double ta = model.ta();
            if (e < 0 || (ta != Constants.INFINITY && e > ta)) {
                report.violated(Invariant.ELAPSED_WITHIN_TA, t, name(), String.format("e=%s ta=%s", e, ta));
            }
        }
        String kind = imminent ? (input ? "con" : "int") : "ext";
        // I2 (second form): a transition function must not write to the output ports
        int outBefore = 0;
        for (Port<?> p : model.getOutPorts()) outBefore += p.getValues().size();
        super.deltfcn();
        int outAfter = 0;
        for (Port<?> p : model.getOutPorts()) outAfter += p.getValues().size();
        report.checked(Invariant.OUTPUT_ONLY_WHEN_IMMINENT);
        if (outAfter > outBefore) {
            report.violated(Invariant.OUTPUT_ONLY_WHEN_IMMINENT, t, name(), "output written inside the " + kind + " transition function (not in lambda)");
        }
        lastEventTime = t;
        checkTa(t, "after " + kind + " transition");
        switch (kind) {
            case "int": digest.deltInts++; break;
            case "ext": digest.deltExts++; break;
            default: digest.deltCons++; break;
        }
        digest.event(t, name(), kind);
    }
}
