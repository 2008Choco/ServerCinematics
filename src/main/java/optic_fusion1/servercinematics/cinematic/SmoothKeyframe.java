package optic_fusion1.servercinematics.cinematic;

import com.google.common.base.Enums;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

import org.bukkit.Location;

public class SmoothKeyframe extends AbstractKeyframe {

    public static final SmoothKeyframe.Serializer SERIALIZER = new Serializer();

    private final double speed;
    private final InterpolationFunction interpolationFunction;

    public SmoothKeyframe(Location location, double wait, double speed, InterpolationFunction interpolationFunction) {
        super(location, wait);

        this.speed = speed;
        this.interpolationFunction = interpolationFunction;

        registerSerializableType(SmoothKeyframe.class, "smooth");
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

    private static final class Serializer extends AbstractKeyframe.Serializer<SmoothKeyframe> {

        @Override
        public JsonElement serialize(SmoothKeyframe keyframe, Type type, JsonSerializationContext context) {
            JsonObject object = super.serialize(keyframe, type, context).getAsJsonObject();

            object.addProperty("speed", keyframe.speed);
            object.addProperty("interpolationFunction", keyframe.interpolationFunction.name());

            return object;
        }

        @Override
        public SmoothKeyframe deserialize(JsonObject object, Type type, JsonDeserializationContext context, CinematicKeyframe parentKeyframe) throws JsonParseException {
            double speed = object.get("speed").getAsDouble();
            InterpolationFunction interpolationFunction = Enums.getIfPresent(InterpolationFunction.class, object.get("interpolationFunction").getAsString()).or(InterpolationFunction.CATMUL_ROM_SPLINE);

            return new SmoothKeyframe(parentKeyframe.getLocation(), parentKeyframe.getWait(), speed, interpolationFunction);
        }

    }

}
