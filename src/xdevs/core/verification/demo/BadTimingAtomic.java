package xdevs.core.verification.demo;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Coupled;
import xdevs.core.modeling.Port;

/**
 * An atomic model with two formalism errors that a unit test of its own methods would not reveal:
 * (1) after the third input it sets a negative time advance (I1); (2) it writes to its output port
 * in the external transition, i.e., outside the output function (I2).
 */
public class BadTimingAtomic extends Atomic {

    public Port<Integer> iIn = new Port<>("in");
    public Port<Integer> oOut = new Port<>("out");
    private int received = 0;

    public BadTimingAtomic(String name) {
        super(name);
        super.addInPort(iIn);
        super.addOutPort(oOut);
    }

    @Override
    public void initialize() {
        super.passivate();
    }

    @Override
    public void exit() {
    }

    @Override
    public void deltint() {
        super.passivate();
    }

    @Override
    public void deltext(double e) {
        super.resume(e);
        received++;
        if (received == 3) {
            super.holdIn("broken", -1.0);          // error 1: negative time advance
        } else {
            super.holdIn("active", 0.5);
        }
        if (received == 2) {
            oOut.addValue(received);               // error 2: output outside lambda
        }
    }

    @Override
    public void lambda() {
        oOut.addValue(received);
    }

    /** Coupled model: a generator feeding the bad atomic. */
    public static Coupled example() {
        Coupled c = new Coupled("BadExample");
        xdevs.core.examples.dynamic.bench.BenchGenerator g = new xdevs.core.examples.dynamic.bench.BenchGenerator("gen", 1.0, 5);
        BadTimingAtomic a = new BadTimingAtomic("bad");
        c.addComponent(g);
        c.addComponent(a);
        c.addCoupling(g.oOut, a.iIn);
        return c;
    }
}
