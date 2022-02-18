package optic_fusion1.servercinematics.cinematic;

import org.bukkit.Location;

public abstract non-sealed class AbstractKeyframe implements CinematicKeyframe {

    private final Location location;
    private final double wait;

    public AbstractKeyframe(Location location, double wait) {
        this.location = location.clone();
        this.wait = wait;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public double getWait() {
        return wait;
    }

}
