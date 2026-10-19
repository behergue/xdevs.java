package xdevs.core.examples.dynamic.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Component;
import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;

/**
 * Micro-benchmark of the structural reconciliation alone (no simulation): a coupled model with
 * {@code w} direct atomic components whose structural transition function removes {@code s}
 * components and, on the next call, adds {@code s} new ones. Measures the cost of one call to
 * {@code CoordinatorDynamic.structuralTransition()} (delta_str + reconciliation) with the
 * list-based and the map-based reconciliation.
 *
 * Usage: ReconcileBench w s calls reps
 * Output (CSV): w,s,reconcile,calls,ns_per_change_median,ns_min,ns_max
 */
public class ReconcileBench {

    /** Passive atomic model: no ports, no events. */
    static class Idle extends Atomic {
        Idle(String name) { super(name); }
        @Override public void initialize() { super.passivate(); }
        @Override public void exit() {}
        @Override public void deltint() {}
        @Override public void deltext(double e) {}
        @Override public void lambda() {}
    }

    /** Pool of w atomics; every structural transition removes s atomics or adds s back. */
    static class Pool extends Coupled {
        final int s;
        final List<Idle> members = new ArrayList<>();
        boolean removeNext = true;
        int created = 0;

        Pool(int w, int s) {
            super("pool");
            this.s = s;
            for (int i = 0; i < w; i++) { Idle a = new Idle("a" + (created++)); members.add(a); super.addComponent(a); }
        }

        @Override
        public boolean structuralTransition() {
            if (removeNext) {
                for (int i = 0; i < s; i++) { Idle a = members.remove(members.size() - 1); super.removeComponent(a); }
            } else {
                for (int i = 0; i < s; i++) { Idle a = new Idle("a" + (created++)); members.add(a); super.addComponent(a); }
            }
            removeNext = !removeNext;
            return true;
        }
    }

    public static void main(String[] args) {
        int w = Integer.parseInt(args[0]);
        int s = Integer.parseInt(args[1]);
        int calls = Integer.parseInt(args[2]);
        int reps = Integer.parseInt(args[3]);
        for (CoordinatorDynamic.Reconcile r : CoordinatorDynamic.Reconcile.values()) {
            double[] ns = new double[reps];
            for (int rep = -1; rep < reps; rep++) {          // rep -1 = warm-up
                Pool pool = new Pool(w, s);
                CoordinatorDynamic c = new CoordinatorDynamic(pool, false, r);
                c.initialize();
                long t0 = System.nanoTime();
                for (int k = 0; k < calls; k++) c.structuralTransition();
                long t1 = System.nanoTime();
                c.exit();
                if (rep >= 0) ns[rep] = (double) (t1 - t0) / calls;
            }
            Arrays.sort(ns);
            System.out.printf("%d,%d,%s,%d,%.0f,%.0f,%.0f%n", w, s, r.name().toLowerCase(), calls, ns[reps / 2], ns[0], ns[reps - 1]);
        }
    }
}
