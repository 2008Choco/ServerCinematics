package optic_fusion1.servercinematics.cinematic;

import org.bukkit.Location;

public class SmoothKeyframe extends AbstractKeyframe {

    private final double speed;
    private final InterpolationFunction interpolationFunction;

    public SmoothKeyframe(Location location, double wait, double speed, InterpolationFunction interpolationFunction) {
        super(location, wait);

        this.speed = speed;
        this.interpolationFunction = interpolationFunction;
    }

    public SmoothKeyframe(Location location, double wait, double speed) {
        this(location, wait, speed, InterpolationFunction.CATMUL_ROM_SPLINE);
    }

    public double getSpeed() {
        return speed;
    }

    public InterpolationFunction getInterpolationFunction() {
        return interpolationFunction;
    }

    public static enum InterpolationFunction {

        CATMUL_ROM_SPLINE;
        // Add more interpolations, maybe? :)

    }

}
