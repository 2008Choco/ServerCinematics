package optic_fusion1.servercinematics.cinematic;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

import org.bukkit.Location;

public final class StaticKeyframe extends AbstractKeyframe {

    public static final StaticKeyframe.Serializer SERIALIZER = new Serializer();

    public StaticKeyframe(Location location, double wait) {
        super(location, wait);

        registerSerializableType(StaticKeyframe.class, "static");
    }

    private static final class Serializer extends AbstractKeyframe.Serializer<StaticKeyframe> {

        @Override
        public JsonElement serialize(StaticKeyframe keyframe, Type type, JsonSerializationContext context) {
            return super.serialize(keyframe, type, context); // No additional data
        }

        @Override
        public StaticKeyframe deserialize(JsonObject object, Type type, JsonDeserializationContext context, CinematicKeyframe parentKeyframe) throws JsonParseException {
            return new StaticKeyframe(parentKeyframe.getLocation(), parentKeyframe.getWait());
        }

    }

}
