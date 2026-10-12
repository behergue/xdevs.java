package xdevs.core.examples.dynamic.bench;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * DEVStone-like atomic model without external dependencies.
 *
 * Behaviour (as DevStoneAtomic with preparation time 0): an external transition
 * schedules an internal transition after {@code preparationTime}; the output
 * function emits one event; the internal transition passivates. An optional
 * synthetic workload ({@code work} iterations of an arithmetic loop) is executed
 * in both transition functions to emulate model computation.
 */
public class BenchAtomic extends Atomic {

    public Port<Integer> iIn = new Port<>("in");
    public Port<Integer> oOut = new Port<>("out");

    protected final double preparationTime;
    protected final int work;

    public long numDeltInts = 0;
    public long numDeltExts = 0;
    public long numOfEvents = 0;
    /** Identifier of the last event received (part of the sequential state). */
    public int lastEvent = 0;

    public static volatile long sink = 0;

    /** Global counters: they survive the removal of atomic models (per-instance counters do not). */
    public static long totalDeltInts = 0;
    public static long totalDeltExts = 0;
    public static long totalEvents = 0;

    public static void resetTotals() {
        totalDeltInts = 0;
        totalDeltExts = 0;
        totalEvents = 0;
    }

    public BenchAtomic(String name, double preparationTime, int work) {
        super(name);
        super.addInPort(iIn);
        super.addOutPort(oOut);
        this.preparationTime = preparationTime;
        this.work = work;
    }

    protected void doWork() {
        long acc = 0;
        for (int i = 0; i < work; i++) {
            acc += (i * 31L) ^ (acc >>> 3);
        }
        sink += acc;
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
        numDeltInts++;
        totalDeltInts++;
        doWork();
        super.passivate();
    }

    @Override
    public void deltext(double e) {
        super.resume(e);
        numDeltExts++;
        totalDeltExts++;
        doWork();
        if (!iIn.isEmpty()) {
            numOfEvents += iIn.getValues().size();
            totalEvents += iIn.getValues().size();
            lastEvent = iIn.getSingleValue();
        }
        super.holdIn("active", preparationTime);
    }

    @Override
    public void lambda() {
        oOut.addValue(lastEvent);
    }
}
