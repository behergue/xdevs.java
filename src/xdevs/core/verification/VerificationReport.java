package xdevs.core.verification;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the violations found during a verified simulation, plus counters per invariant.
 */
public class VerificationReport {

    public static class Violation {
        public final Invariant invariant;
        public final double time;
        public final String component;
        public final String detail;

        Violation(Invariant invariant, double time, String component, String detail) {
            this.invariant = invariant;
            this.time = time;
            this.component = component;
            this.detail = detail;
        }

        @Override
        public String toString() {
            return String.format("[%s] t=%s %s: %s", invariant, time, component, detail);
        }
    }

    public final List<Violation> violations = new ArrayList<>();
    public final Map<Invariant, Long> checks = new EnumMap<>(Invariant.class);
    public final int maxViolationsPerInvariant;

    public VerificationReport() {
        this(20);
    }

    public VerificationReport(int maxViolationsPerInvariant) {
        this.maxViolationsPerInvariant = maxViolationsPerInvariant;
    }

    void checked(Invariant inv) {
        checks.merge(inv, 1L, Long::sum);
    }

    void violated(Invariant inv, double time, String component, String detail) {
        long already = violations.stream().filter(v -> v.invariant == inv).count();
        if (already < maxViolationsPerInvariant) {
            violations.add(new Violation(inv, time, component, detail));
        }
    }

    public boolean ok() {
        return violations.isEmpty();
    }

    public long count(Invariant inv) {
        return violations.stream().filter(v -> v.invariant == inv).count();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ok() ? "VERIFICATION PASSED" : "VERIFICATION FAILED").append('\n');
        for (Invariant inv : Invariant.values()) {
            long c = checks.getOrDefault(inv, 0L);
            long v = count(inv);
            if (c > 0 || v > 0) {
                sb.append(String.format("  %-28s checks=%-9d violations=%d%n", inv, c, v));
            }
        }
        for (Violation v : violations) {
            sb.append("  ").append(v).append('\n');
        }
        return sb.toString();
    }
}
