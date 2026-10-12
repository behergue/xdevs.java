package xdevs.core.examples.dynamic.bench;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.logging.Level;

import xdevs.core.examples.dynamic.Factory;
import xdevs.core.examples.dynamic.Machine;
import xdevs.core.examples.efp.Transducer;
import xdevs.core.modeling.Component;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;
import xdevs.core.util.DevsLogger;

/**
 * Case study measurements for the dynamic Factory.
 *
 * Mode "sweep": for several values of maxMachines, prints jobs arrived/solved, structural changes
 *               and the time-average number of machines.
 * Mode "trace": prints the number of machines after every time unit for one configuration.
 *
 * Usage: FactoryStudy sweep period processingTime observationTime
 *        FactoryStudy trace period processingTime observationTime maxMachines
 */
public class FactoryStudy {

    static class ObservableFactory extends Factory {
        int changes = 0;
        ObservableFactory(String name, double period, double processingTime, double observationTime, int maxMachines) {
            super(name, period, processingTime, observationTime, maxMachines);
        }
        @Override
        public boolean structuralTransition() {
            boolean c = super.structuralTransition();
            if (c) changes++;
            return c;
        }
        int numMachines() { return machines.size(); }
        int numIdle() {
            int n = 0;
            for (Machine m : machines) if (m.phaseIs(Machine.PHASE_IDLE)) n++;
            return n;
        }
    }

    @SuppressWarnings("unchecked")
    static int transducerCount(ObservableFactory f, String field) throws Exception {
        for (Component c : f.getComponents()) {
            if (c instanceof Transducer) {
                Field fld = Transducer.class.getDeclaredField(field);
                fld.setAccessible(true);
                return ((LinkedList<Object>) fld.get(c)).size();
            }
        }
        return -1;
    }

    static boolean LAZY = true;

    public static void main(String[] args) throws Exception {
        DevsLogger.setup(Level.SEVERE);
        String mode = args[0];
        LAZY = !(args.length > 5 && args[5].equals("eager"));
        double period = Double.parseDouble(args[1]);
        double processingTime = Double.parseDouble(args[2]);
        double observationTime = Double.parseDouble(args[3]);
        if (mode.equals("sweep")) {
            System.out.println("maxMachines,jobsArrived,jobsSolved,structuralChanges,avgMachines,maxMachinesReached,throughput");
            for (int max : new int[] {1, 2, 3, 4, 5, 6, 8}) {
                ObservableFactory f = new ObservableFactory("factory", period, processingTime, observationTime, max);
                CoordinatorDynamic c = new CoordinatorDynamic(f, LAZY, CoordinatorDynamic.Reconcile.MAP);
                c.initialize();
                double sumMachines = 0; int steps = 0; int peak = 0;
                while (c.getClock().getTime() < observationTime) {
                    c.simulate(1.0);
                    sumMachines += f.numMachines(); steps++;
                    peak = Math.max(peak, f.numMachines());
                }
                int arrived = transducerCount(f, "jobsArrived");
                int solved = transducerCount(f, "jobsSolved");
                System.out.printf("%d,%d,%d,%d,%.3f,%d,%.3f%n", max, arrived, solved, f.changes,
                        sumMachines / steps, peak, solved / observationTime);
                c.exit();
            }
        } else {
            int max = Integer.parseInt(args[4]);
            ObservableFactory f = new ObservableFactory("factory", period, processingTime, observationTime, max);
            CoordinatorDynamic c = new CoordinatorDynamic(f, LAZY, CoordinatorDynamic.Reconcile.MAP);
            c.initialize();
            System.out.println("t,machines,idle,jobsArrived,jobsSolved");
            while (c.getClock().getTime() < observationTime) {
                c.simulate(1.0);
                System.out.printf("%.1f,%d,%d,%d,%d%n", c.getClock().getTime(), f.numMachines(), f.numIdle(),
                        transducerCount(f, "jobsArrived"), transducerCount(f, "jobsSolved"));
            }
            c.exit();
        }
    }
}
