package optic_fusion1.servercinematics.cinematic;

import org.bukkit.Location;

public sealed interface CinematicKeyframe permits AbstractKeyframe {

    public Location getLocation();

    public double getWait();

}
