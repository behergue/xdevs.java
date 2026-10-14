package xdevs.core.verification.demo;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Coupled;

/**
 * A coupled model whose components never transition (they are passive from the start) but whose
 * structural transition function toggles a second component on and off at every evaluation. Placed
 * next to an active model, it is evaluated in cycles in which none of its children transitioned, and
 * changes its structure there: a direct violation of the stability assumption (I7c) that makes the
 * simulation of this coupled model depend on the events of unrelated models.
 */
public class QuietToggler extends Coupled {

    static class Passive extends Atomic {
        Passive(String name) { super(name); }
        @Override public void initialize() { super.passivate(); }
        @Override public void exit() {}
        @Override public void deltint() { super.passivate(); }
        @Override public void deltext(double e) { super.passivate(); }
        @Override public void lambda() {}
    }

    private Passive extra = null;
    private int n = 0;

    public QuietToggler(String name) {
        super(name);
        super.addComponent(new Passive("p0"));
    }

    @Override
    public boolean structuralTransition() {
        if (extra == null) {
            extra = new Passive("p" + (++n));
            super.addComponent(extra);
        } else {
            super.removeComponent(extra);
            extra = null;
        }
        return true;
    }
}
