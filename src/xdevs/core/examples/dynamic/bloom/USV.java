package xdevs.core.examples.dynamic.bloom;

import xdevs.core.modeling.Atomic;
import xdevs.core.modeling.Port;

/**
 * Unmanned Surface Vehicle: drains its battery while it patrols and sends telemetry every hour.
 * When the battery goes below 10% it enters the phase "depleted" (passive): the fog removes it and,
 * if the demand persists and the fleet has spare vehicles, launches another one.
 */
public class USV extends Atomic {

    public static final String PHASE_PATROL = "patrol";
    public static final String PHASE_DEPLETED = "depleted";

    public Port<Events.Telemetry> oTelemetry = new Port<>("oTelemetry");

    private double battery = 100.0;
    private final double drainPerHour;

    public USV(String name, double drainPerHour) {
        super(name);
        super.addOutPort(oTelemetry);
        this.drainPerHour = drainPerHour;
    }

    @Override
    public void initialize() { super.holdIn(PHASE_PATROL, 1.0); }

    @Override
    public void exit() {}

    @Override
    public void deltint() {
        if (phaseIs(PHASE_DEPLETED)) { super.passivate(PHASE_DEPLETED); return; }
        battery -= drainPerHour;
        if (battery < 10.0) super.holdIn(PHASE_DEPLETED, 0.0); // last telemetry, then passive
        else super.holdIn(PHASE_PATROL, 1.0);
    }

    @Override
    public void deltext(double e) { super.resume(e); }

    @Override
    public void lambda() { oTelemetry.addValue(new Events.Telemetry(getName(), battery)); }
}
