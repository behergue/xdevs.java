package xdevs.core.verification;

/**
 * DEVS-level invariants checked offline by the verifying simulators. They are properties of the
 * formalism (PDEVS and dynamic structure PDEVS), not unit tests of a particular model.
 */
public enum Invariant {
    /** I1. The time advance of every reachable state is non-negative (infinity allowed). */
    TA_NON_NEGATIVE("ta(s) >= 0 for every reachable state"),
    /** I2. An atomic model produces output only when it is imminent (lambda is called right before deltint/deltcon). */
    OUTPUT_ONLY_WHEN_IMMINENT("output ports are non-empty only at the time of next event"),
    /** I3. The elapsed time passed to deltext satisfies 0 <= e <= ta(s), i.e., the total state stays in Q. */
    ELAPSED_WITHIN_TA("0 <= e <= ta(s) in every external transition"),
    /** I4. Event times never decrease and tL <= t <= tN at every transition. */
    TIME_MONOTONIC("event times are non-decreasing and tL <= t <= tN"),
    /** I5. Legitimacy (bounded): no more than N consecutive cycles at the same simulation time. */
    LEGITIMACY_BOUNDED("bounded number of zero-time cycles at the same instant"),
    /** I6. After every structural phase the structure is well formed: couplings refer to ports of current components, no self-couplings, simulators = components. */
    STRUCTURE_WELL_FORMED("couplings and simulators consistent with the current components"),
    /** I7a. Stability (c): the initial structure is a fixed point of delta_str. */
    STABILITY_INITIAL("delta_str does not change the initial structure"),
    /** I7b. Stability (b): right after a structural change, delta_str returns false (idempotence). */
    STABILITY_IDEMPOTENT("delta_str is idempotent after a change"),
    /** I7c. Stability (a), observable form: in a cycle where no child transitioned, delta_str must not change the structure. */
    STABILITY_QUIET_CYCLE("delta_str does not change the structure in a quiet cycle"),
    /** I8. Eager and lazy policies produce the same trace (transition counts, structural changes, outputs). */
    EAGER_LAZY_EQUIVALENCE("eager and lazy policies yield identical traces"),
    /** I9. Optional: transition counts match a closed-form expectation supplied by the model. */
    EXPECTED_COUNTS("transition counts match the theoretical values");

    public final String description;

    Invariant(String description) {
        this.description = description;
    }
}
