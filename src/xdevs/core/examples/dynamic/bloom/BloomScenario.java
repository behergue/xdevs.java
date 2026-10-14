package xdevs.core.examples.dynamic.bloom;

import java.util.function.Supplier;
import java.util.logging.Level;

import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;
import xdevs.core.util.DevsLogger;
import xdevs.core.verification.Verifier;

/**
 * Dynamic DEVS-BLOOM prototype: a water body in which blooms appear and disappear, and a fog layer
 * that creates one tracker per detected bloom and launches/retires USVs according to the demand and
 * their batteries. Root coupled model: WaterBody -> Fog.
 *
 * Usage: BloomScenario [hours] [fleetSize] [maxSimultaneousUsvs] [nutrients]
 */
public class BloomScenario extends Coupled {

    public final WaterBody water;
    public final Fog fog;

    public BloomScenario(double[] birthHours, double nutrients, int fleetSize, int maxSimultaneous, double usvDrain) {
        super("DEVS-BLOOM-DS");
        water = new WaterBody("lake", birthHours, nutrients);
        fog = new Fog("fog", fleetSize, maxSimultaneous, usvDrain);
        super.addComponent(water);
        super.addComponent(fog);
        super.addCoupling(water.oReports, fog.iReports);
    }

    /** Two weeks of Lake Washington, August 2008 (as in the DEVS-BLOOM use case): births at these hours. */
    public static double[] defaultBirths() {
        return new double[] {8, 10, 33, 60, 62, 100, 130, 131, 132, 180, 220, 260, 290, 300};
    }

    public static void main(String[] args) {
        DevsLogger.setup(Level.SEVERE);
        double hours = args.length > 0 ? Double.parseDouble(args[0]) : 24 * 15;
        int fleet = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int maxSim = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        double nutrients = args.length > 3 ? Double.parseDouble(args[3]) : 1.0;
        Supplier<Coupled> factory = () -> new BloomScenario(defaultBirths(), nutrients, fleet, maxSim, 2.5);

        // 1) Simulation with the lazy dynamic coordinator, printing the structure every 12 h
        BloomScenario m = (BloomScenario) factory.get();
        CoordinatorDynamic c = new CoordinatorDynamic(m, true, CoordinatorDynamic.Reconcile.MAP);
        c.initialize();
        System.out.println("hour,blooms,trackers,usvs,launched,bornTotal,fadedTotal");
        double t = 0;
        while (t < hours) {
            c.simulate(12.0);
            t += 12.0;
            System.out.printf("%.0f,%d,%d,%d,%d,%d,%d%n", t, m.water.activeBlooms(), m.fog.activeTrackers(), m.fog.activeUsvs(),
                    m.fog.launched(), m.water.bornTotal, m.water.fadedTotal);
        }
        c.exit();
        System.out.printf("structural evaluations=%d changes=%d added=%d removed=%d | trackers created=%d removed=%d | usvs removed=%d | gcs reports=%d estimates=%d telemetries=%d%n",
                CoordinatorDynamic.structuralEvaluations, CoordinatorDynamic.structuralChanges, CoordinatorDynamic.componentsAdded,
                CoordinatorDynamic.componentsRemoved, m.fog.trackersCreated, m.fog.trackersRemoved, m.fog.usvsRemoved,
                m.fog.gcs.reports, m.fog.gcs.estimates, m.fog.gcs.telemetries);

        // 2) Offline verification of the formalism invariants, including stability and eager/lazy equivalence
        System.out.println("==== verification");
        Verifier.Result r = Verifier.verify(factory, 100_000);
        Verifier.verifyEagerLazyEquivalence(factory, 100_000, r.report);
        System.out.println("trace: " + r.digest);
        System.out.print(r.report);
    }
}
