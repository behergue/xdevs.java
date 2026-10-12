package xdevs.core.examples.dynamic.bench;

import java.util.Arrays;
import java.util.logging.Level;

import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.Coordinator;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;
import xdevs.core.util.DevsLogger;

/**
 * Benchmark harness for dynamic structure DEVS in xDEVS.
 *
 * Usage: DynBench model width depth maxEvents work coordinator reps [churnPeriod churnSize]
 *   model:       LI | HI | HO | DHO
 *   coordinator: static | eager-list | eager-map | lazy-list | lazy-map
 *
 * Prints one CSV line:
 *   model,width,depth,maxEvents,work,coordinator,churnPeriod,churnSize,idleCopies,atomicsInitial,atomicsFinal,
 *   deltInts,deltExts,events,theoryDeltExts,structEvals,structChanges,added,removed,
 *   t_median_ms,t_min_ms,t_max_ms
 */
public class DynBench {

    static Coupled build(String model, int width, int depth, int work, int churnPeriod, int churnSize) {
        Coupled framework = new Coupled("Framework");
        BenchGenerator generator = new BenchGenerator("Generator", 1.0, maxEvents);
        framework.addComponent(generator);
        BenchStone stone;
        switch (model) {
            case "LI": stone = new BenchLI("C", width, depth, 0.0, work); break;
            case "HI": stone = new BenchHI("C", width, depth, 0.0, work); break;
            case "HO": stone = new BenchHO("C", width, depth, 0.0, work); break;
            case "DHO": stone = new DynHO("C", width, depth, 0.0, work, churnPeriod, churnSize); break;
            default: throw new IllegalArgumentException("model " + model);
        }
        framework.addComponent(stone);
        framework.addCoupling(generator.oOut, stone.iIn);
        if (stone instanceof BenchHO) framework.addCoupling(generator.oOut, ((BenchHO) stone).iInAux);
        if (stone instanceof DynHO) framework.addCoupling(generator.oOut, ((DynHO) stone).iInAux);
        // Idle copies: identical subsystems that never receive events (quiet subtrees)
        for (int i = 0; i < idleCopies; i++) {
            framework.addComponent(new BenchHO("I" + i + "_", width, depth, 0.0, work));
        }
        return framework;
    }

    static int maxEvents;
    static int idleCopies = 0;

    static Coordinator coordinator(String kind, Coupled model) {
        switch (kind) {
            case "static": return new Coordinator(model, false);
            case "eager-list": return new CoordinatorDynamic(model, false, CoordinatorDynamic.Reconcile.LIST);
            case "eager-map": return new CoordinatorDynamic(model, false, CoordinatorDynamic.Reconcile.MAP);
            case "lazy-list": return new CoordinatorDynamic(model, true, CoordinatorDynamic.Reconcile.LIST);
            case "lazy-map": return new CoordinatorDynamic(model, true, CoordinatorDynamic.Reconcile.MAP);
            default: throw new IllegalArgumentException("coordinator " + kind);
        }
    }

    public static void main(String[] args) {
        DevsLogger.setup(Level.SEVERE);
        String model = args[0];
        int width = Integer.parseInt(args[1]);
        int depth = Integer.parseInt(args[2]);
        maxEvents = Integer.parseInt(args[3]);
        int work = Integer.parseInt(args[4]);
        String kind = args[5];
        int reps = Integer.parseInt(args[6]);
        int churnPeriod = args.length > 7 ? Integer.parseInt(args[7]) : 0;
        int churnSize = args.length > 8 ? Integer.parseInt(args[8]) : 0;
        idleCopies = args.length > 9 ? Integer.parseInt(args[9]) : 0;

        // Warm-up (JIT)
        for (int i = 0; i < 2; i++) {
            Coupled m = build(model, width, depth, work, churnPeriod, churnSize);
            Coordinator c = coordinator(kind, m);
            c.initialize();
            c.simulate(Long.MAX_VALUE);
            c.exit();
        }

        double[] times = new double[reps];
        long deltInts = 0, deltExts = 0, events = 0, theory = 0;
        int atomicsInitial = 0, atomicsFinal = 0;
        long structEvals = 0, structChanges = 0, added = 0, removed = 0;
        for (int r = 0; r < reps; r++) {
            Coupled m = build(model, width, depth, work, churnPeriod, churnSize);
            BenchStone stone = (BenchStone) m.getComponentByName("C" + (depth - 1));
            atomicsInitial = stone.countAtomicComponents();
            CoordinatorDynamic.resetStatistics();
            BenchAtomic.resetTotals();
            Coordinator c = coordinator(kind, m);
            c.initialize();
            System.gc();
            long t0 = System.nanoTime();
            c.simulate(Long.MAX_VALUE);
            long t1 = System.nanoTime();
            c.exit();
            times[r] = (t1 - t0) / 1e6;
            // Global counters: per-instance counters are lost when a component is removed
            deltInts = BenchAtomic.totalDeltInts;
            deltExts = BenchAtomic.totalDeltExts;
            events = BenchAtomic.totalEvents;
            theory = stone.numDeltExtsInTheory(maxEvents, width, depth);
            atomicsFinal = stone.countAtomicComponents();
            structEvals = CoordinatorDynamic.structuralEvaluations;
            structChanges = CoordinatorDynamic.structuralChanges;
            added = CoordinatorDynamic.componentsAdded;
            removed = CoordinatorDynamic.componentsRemoved;
        }
        Arrays.sort(times);
        double median = times[reps / 2];
        System.out.printf("%s,%d,%d,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f%n",
                model, width, depth, maxEvents, work, kind, churnPeriod, churnSize, idleCopies, atomicsInitial, atomicsFinal,
                deltInts, deltExts, events, theory, structEvals, structChanges, added, removed,
                median, times[0], times[reps - 1]);
    }
}
