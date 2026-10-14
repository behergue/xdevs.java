package xdevs.core.verification.demo;

import xdevs.core.examples.dynamic.Factory;
import xdevs.core.examples.dynamic.Machine;

/**
 * A deliberately wrong variant of the Factory: its structural transition function adds a machine
 * whenever the number of machines is odd and removes the last one whenever it is even, regardless of
 * the state of the machines. It violates the stability assumption: it is not idempotent (I7b) and it
 * changes the structure in quiet cycles (I7c), and it is not a fixed point initially (I7a).
 */
public class UnstableFactory extends Factory {

    private int next = 100;

    public UnstableFactory(String name, double period, double processingTime, double observationTime, int maxMachines) {
        super(name, period, processingTime, observationTime, maxMachines);
    }

    @Override
    public boolean structuralTransition() {
        if (machines.size() % 2 == 1) {
            Machine m = new Machine("machine-" + (next++), processingTime);
            super.addComponent(m);
            Machine prev = machines.get(machines.size() - 1);
            super.addCoupling(prev.oJobIgnored, m.iJob);
            super.addCoupling(m.oJobSolved, transducer.iSolved);
            machines.add(m);
        } else {
            Machine last = machines.remove(machines.size() - 1);
            super.removeComponent(last);
        }
        return true;
    }
}
