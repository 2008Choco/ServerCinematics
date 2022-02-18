package optic_fusion1.servercinematics.cinematic;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public abstract non-sealed class AbstractKeyframe implements CinematicKeyframe {

    private static final BiMap<String, Type> SERIALIZABLE_TYPE = HashBiMap.create();

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

    protected static void registerSerializableType(Type type, String id) {
        SERIALIZABLE_TYPE.put(id, type);
    }

    static String getSerializableTypeId(Type type) {
        return SERIALIZABLE_TYPE.inverse().get(type);
    }

    static Type getSerializableType(String id) {
        return SERIALIZABLE_TYPE.get(id);
    }

    protected static abstract class Serializer<T extends AbstractKeyframe> implements JsonSerializer<T>, JsonDeserializer<T> {

        @Override
        public JsonElement serialize(T keyframe, Type type, JsonSerializationContext context) {
            String typeId = getSerializableTypeId(keyframe.getClass());
            if (typeId == null) {
                throw new UnsupportedOperationException(keyframe.getClass().getName() + " is not serializable. Ensure that registerSerializableType() has been called for this implementation.");
            }

            JsonObject object = new JsonObject();

            object.addProperty("type", typeId);

            // Location
            Location location = keyframe.getLocation();
            World world = location.getWorld();

            if (world == null) {
                throw new UnsupportedOperationException("Keyframe world is null.");
            }

            object.addProperty("world", world.getName());
            object.addProperty("x", location.getX());
            object.addProperty("y", location.getY());
            object.addProperty("z", location.getZ());
            object.addProperty("yaw", location.getYaw());
            object.addProperty("pitch", location.getPitch());

            // Wait time
            object.addProperty("wait", keyframe.getWait());

            return object;
        }

        public abstract T deserialize(JsonObject object, Type type, JsonDeserializationContext context, CinematicKeyframe parentKeyframe) throws JsonParseException;

        @Override
        public final T deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (!element.isJsonObject()) {
                throw new JsonParseException("Expected JsonObject (keyframe), got " + element.getClass().getSimpleName());
            }

            JsonObject object = element.getAsJsonObject();

            World world = Bukkit.getWorld(object.get("world").getAsString());
            if (world == null) {
                throw new JsonParseException("Unknown world, got \"" + object.get("world").getAsString() + "\"");
            }

            double x = object.get("x").getAsDouble();
            double y = object.get("y").getAsDouble();
            double z = object.get("z").getAsDouble();
            float yaw = object.get("yaw").getAsFloat();
            float pitch = object.get("pitch").getAsFloat();

            double wait = object.get("wait").getAsDouble();

            return deserialize(object, type, context, new CinematicKeyframeDeserializationContext(new Location(world, x, y, z, yaw, pitch), wait));
        }

    }

    // Used only for deserialization
    private static class CinematicKeyframeDeserializationContext extends AbstractKeyframe {

        private CinematicKeyframeDeserializationContext(Location location, double wait) {
            super(location, wait);
        }

    }

}
