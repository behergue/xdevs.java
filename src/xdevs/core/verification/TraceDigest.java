package xdevs.core.verification;

import java.util.Objects;
import java.util.TreeMap;

/**
 * Compact, order-independent summary of a simulation run, used to compare two executions
 * (e.g., eager against lazy policy) without storing the full trace.
 */
public class TraceDigest {
    public long deltInts = 0, deltExts = 0, deltCons = 0, outputs = 0;
    public long structuralChanges = 0, componentsAdded = 0, componentsRemoved = 0;
    /** Rolling hash of (time, component, kind) events, order-sensitive. */
    public long eventHash = 17;
    /** Transitions per component name (names survive removal/re-creation). */
    public final TreeMap<String, Long> perComponent = new TreeMap<>();

    void event(double t, String component, String kind) {
        eventHash = eventHash * 1000003L + Double.hashCode(t);
        eventHash = eventHash * 1000003L + component.hashCode();
        eventHash = eventHash * 1000003L + kind.hashCode();
        perComponent.merge(component, 1L, Long::sum);
    }

    public boolean sameAs(TraceDigest o) {
        return deltInts == o.deltInts && deltExts == o.deltExts && deltCons == o.deltCons && outputs == o.outputs
                && structuralChanges == o.structuralChanges && componentsAdded == o.componentsAdded
                && componentsRemoved == o.componentsRemoved && eventHash == o.eventHash
                && Objects.equals(perComponent, o.perComponent);
    }

    @Override
    public String toString() {
        return String.format("int=%d ext=%d con=%d out=%d structChanges=%d added=%d removed=%d hash=%d",
                deltInts, deltExts, deltCons, outputs, structuralChanges, componentsAdded, componentsRemoved, eventHash);
    }
}
