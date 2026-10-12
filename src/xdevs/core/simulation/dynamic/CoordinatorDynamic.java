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
 *  - Beatriz Herguedas Pinedo (lazy evaluation, map-based reconciliation, statistics)
 */
package xdevs.core.simulation.dynamic;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import xdevs.core.examples.efp.Efp;
import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Component;
import xdevs.core.modeling.Coupled;
import xdevs.core.simulation.AbstractSimulator;
import xdevs.core.simulation.Coordinator;
import xdevs.core.simulation.SimulationClock;
import xdevs.core.simulation.Simulator;
import xdevs.core.util.DevsLogger;

/**
 * Coordinator for dynamic structure coupled models.
 *
 * A simulation cycle at time t is: output phase (lambda), transition phase
 * (deltfcn of every child) and, new with respect to {@link Coordinator},
 * a structural phase in which {@link Coupled#structuralTransition()} is
 * evaluated on the post-transition states and the set of simulators is
 * reconciled with the (possibly new) set of components. Structural phases run
 * bottom-up: a child coordinator finishes its own structural phase inside the
 * transition phase of its parent.
 *
 * Two policies are available:
 * <ul>
 * <li><b>eager</b> (original behaviour): the structural transition function is
 * evaluated in every cycle of every coupled model.</li>
 * <li><b>lazy</b>: it is evaluated only in cycles in which at least one direct
 * child performed a transition. Under the stability assumption of the paper
 * this is semantics-preserving (Lemma 1), and removes the evaluation cost from
 * quiet cycles.</li>
 * </ul>
 */
public class CoordinatorDynamic extends Coordinator {

    /**
     * Reconciliation strategy between components and simulators.
     */
    public enum Reconcile {
        /** Original implementation: list scans, O(|components| x |simulators|). */
        LIST,
        /** Identity map component -> simulator, O(|components|). */
        MAP
    }

    protected final boolean lazy;
    protected final Reconcile reconcile;
    /** Simulator associated with each component (MAP strategy only). */
    protected final Map<Component, AbstractSimulator> simulatorOf = new IdentityHashMap<>();
    /** True when at least one direct child transitioned in the current cycle. */
    protected boolean childTransitioned = false;
    /** True when this coordinator transitioned (a child transitioned or the structure changed) in the current cycle. */
    protected boolean transitioned = false;

    // Statistics, aggregated over the whole hierarchy through the static counters.
    public static long structuralEvaluations = 0;
    public static long structuralChanges = 0;
    public static long componentsAdded = 0;
    public static long componentsRemoved = 0;

    public static void resetStatistics() {
        structuralEvaluations = 0;
        structuralChanges = 0;
        componentsAdded = 0;
        componentsRemoved = 0;
    }

    public CoordinatorDynamic(SimulationClock clock, Coupled model, boolean lazy, Reconcile reconcile) {
        super(clock, model, false);
        this.lazy = lazy;
        this.reconcile = reconcile;
    }

    public CoordinatorDynamic(SimulationClock clock, Coupled model) {
        this(clock, model, false, Reconcile.LIST);
    }

    public CoordinatorDynamic(Coupled model, boolean lazy, Reconcile reconcile) {
        this(new SimulationClock(), model, lazy, reconcile);
    }

    public CoordinatorDynamic(Coupled model) {
        this(model, false, Reconcile.LIST);
    }

    @Override
    protected void buildHierarchy() {
        Collection<Component> components = model.getComponents();
        components.forEach((component) -> {
            AbstractSimulator simulator = newSimulator(component);
            simulators.add(simulator);
            simulatorOf.put(component, simulator);
        });
    }

    protected AbstractSimulator newSimulator(Component component) {
        if (component instanceof Coupled) {
            return new CoordinatorDynamic(clock, (Coupled) component, lazy, reconcile);
        }
        return new Simulator(clock, (Atomic) component);
    }

    @Override
    public void deltfcn() {
        double t = clock.getTime();
        propagateInput();
        childTransitioned = false;
        for (AbstractSimulator simulator : simulators) {
            simulator.deltfcn();
            if (hasTransitioned(simulator, t)) {
                childTransitioned = true;
            }
        }
        boolean changed = false;
        if (!lazy || childTransitioned) {
            changed = this.structuralTransition();
        }
        transitioned = childTransitioned || changed;
        tL = t;
        tN = tL + ta();
    }

    /**
     * Whether a child simulator performed a transition at time t.
     */
    protected boolean hasTransitioned(AbstractSimulator simulator, double t) {
        if (simulator instanceof CoordinatorDynamic) {
            return ((CoordinatorDynamic) simulator).transitioned;
        }
        // An atomic simulator updates tL only when a transition function was executed.
        return simulator.getTL() == t;
    }

    /**
     * Structural phase: evaluates the structural transition function of the model and
     * reconciles the simulators with the components.
     * @return true if the structure changed.
     */
    public boolean structuralTransition() {
        structuralEvaluations++;
        if (!model.structuralTransition()) {
            return false;
        }
        structuralChanges++;
        if (reconcile == Reconcile.MAP) {
            reconcileWithMap();
        } else {
            reconcileWithLists();
        }
        return true;
    }

    /** Original reconciliation (list scans). */
    private void reconcileWithLists() {
        Collection<Component> components = model.getComponents();
        Iterator<AbstractSimulator> it = simulators.iterator();
        while (it.hasNext()) {
            AbstractSimulator simulator = it.next();
            if (!components.contains(simulator.getModel())) {
                simulator.exit();
                it.remove();
                componentsRemoved++;
            }
        }
        components.forEach(component -> {
            if (simulators.stream().noneMatch(simulator -> simulator.getModel() == component)) {
                AbstractSimulator simulator = newSimulator(component);
                simulator.initialize();
                simulators.add(simulator);
                componentsAdded++;
            }
        });
    }

    /** Reconciliation with an identity map component -> simulator. */
    private void reconcileWithMap() {
        Collection<Component> components = model.getComponents();
        // Mark the current components
        Map<Component, Boolean> present = new IdentityHashMap<>(components.size() * 2);
        for (Component c : components) {
            present.put(c, Boolean.TRUE);
        }
        // Remove the simulators of removed components
        Iterator<AbstractSimulator> it = simulators.iterator();
        while (it.hasNext()) {
            AbstractSimulator simulator = it.next();
            if (!present.containsKey(simulator.getModel())) {
                simulator.exit();
                simulatorOf.remove(simulator.getModel());
                it.remove();
                componentsRemoved++;
            }
        }
        // Add simulators for new components
        for (Component c : components) {
            if (!simulatorOf.containsKey(c)) {
                AbstractSimulator simulator = newSimulator(c);
                simulator.initialize();
                simulators.add(simulator);
                simulatorOf.put(c, simulator);
                componentsAdded++;
            }
        }
    }

    public static void main(String[] args) {
        DevsLogger.setup(Level.INFO);
        Efp efp = new Efp("EFP", 1, 3, 1000000);
        CoordinatorDynamic coordinator = new CoordinatorDynamic(efp);
        coordinator.initialize();
        coordinator.simulate(Long.MAX_VALUE);
        coordinator.exit();
    }
}
