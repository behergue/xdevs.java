/*
* Copyright (C) 2023 José Luis Risco Martín <jlrisco@ucm.es>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
* Contributors:
*  - José Luis Risco Martín
*/

package xdevs.core.examples.dynamic;

import java.util.ArrayList;
import java.util.logging.Level;

import xdevs.core.examples.efp.Generator;
import xdevs.core.examples.efp.Transducer;
import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.dynamic.CoordinatorDynamic;
import xdevs.core.util.DevsLogger;

/**
 * Factory with dynamic number of machines.
 * 
 * The factory has a generator, a transducer and a variable number of machines.
 * 
 * When a job arrives, the factory sends it to the first idle machine.
 * If there are no idle machines, a new machine is created and incorporated to the factory.
 */
public class Factory extends Coupled {

    protected ArrayList<Machine> machines = new ArrayList<>();
    protected double processingTime;
    protected Transducer transducer;
    protected int maxMachines;
    /** Monotonic counter to give every machine a unique name, even after removals. */
    protected int nextMachineId = 1;

    /**
     * Constructor
     * @param name Factory name
     * @param period Generator period
     * @param processingTime Machine processing time
     * @param observationTime Transducer observation time
     * @param maxMachines Maximum number of machines
     */
    public Factory(String name, double period, double processingTime, double observationTime, int maxMachines) {
    	super(name);
        this.processingTime = processingTime;
        this.maxMachines = maxMachines;
        // Generator
        Generator generator = new Generator("generator", period);
        super.addComponent(generator);
        // At least one machine
        Machine machine0 = new Machine("machine-0", processingTime);
        machines.add(machine0);
        super.addComponent(machine0);
        /*Machine machine1 = new Machine("machine-1", processingTime);
        machines.add(machine1);
        super.addComponent(machine1);*/
        // Transducer
        transducer = new Transducer("transducer", observationTime);
        super.addComponent(transducer);
        // Couplings
        super.addCoupling(generator.oOut, machine0.iJob);
        super.addCoupling(generator.oOut, transducer.iArrived);
        super.addCoupling(machine0.oJobSolved, transducer.iSolved);
        //super.addCoupling(machine0.oJobIgnored, machine1.iJob);
        //super.addCoupling(machine1.oJobSolved, transducer.iSolved);
        super.addCoupling(transducer.oOut, generator.iStop);
    }

    @Override
    public boolean structuralTransition() {
        boolean change = false;
        // Count idle machines:
        int numIdle = 0;
        for(Machine machine : machines) {
            if(machine.phaseIs(Machine.PHASE_IDLE)) {
                numIdle++;
            }
        }
        // Remove idle machines and couplings, keeping at least one:
        for (int i = machines.size() - 1; (i >= 0) && (numIdle > 1); i--) {
            Machine machine = machines.get(i);
            if (machine.phaseIs(Machine.PHASE_IDLE)) {
                Machine prevMachine = machines.get(i - 1);
                Machine nextMachine = (machines.size() > i + 1) ? machines.get(i + 1) : null;
                if (nextMachine != null)
                    super.addCoupling(prevMachine.oJobIgnored, nextMachine.iJob);
                machines.remove(machine);
                super.removeComponent(machine);
                numIdle--;
                change = true;
            }
        }
        if (numIdle==0 && machines.size()<maxMachines) {
            Machine machine = new Machine("machine-" + (nextMachineId++), processingTime);
            super.addComponent(machine);
            Machine prevMachine = machines.get(machines.size()-1);
            super.addCoupling(prevMachine.oJobIgnored, machine.iJob);
            super.addCoupling(machine.oJobSolved, transducer.iSolved);
            machines.add(machine);
            change = true;
        }
        return change;
    }

    public static void main(String args[]) {
        DevsLogger.setup(Level.FINE);
        Factory factory = new Factory("factory", 1.0, 3.0, 100.0, 2);
        CoordinatorDynamic coordinator = new CoordinatorDynamic(factory);
        coordinator.initialize();
        coordinator.simulate(Long.MAX_VALUE);
        coordinator.exit();
    }
}
