package optic_fusion1.servercinematics.cinematic;

import com.google.common.base.Enums;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CinematicPath implements Cloneable, Iterable<CinematicKeyframe> {

    public static final CinematicPath.Serializer SERIALIZER = new Serializer();

    private final String id;
    private final UUID ownerUUID;
    private final LocalDateTime creationDate;

    private final CinematicKeyframe[] keyframes;
    private final CinematicCompletion completion;

    private final boolean playerFreelookAllowed;

    private CinematicPath(String id, UUID ownerUUID, LocalDateTime creationDate, CinematicKeyframe[] keyframes, CinematicCompletion completion, boolean playerFreelookAllowed) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.creationDate = creationDate;

        this.keyframes = keyframes;
        this.completion = completion;

        this.playerFreelookAllowed = playerFreelookAllowed;
    }

    private CinematicPath(CinematicPath other) {
        this(other.id, other.ownerUUID, other.creationDate, other.getKeyframes(), other.completion, other.playerFreelookAllowed);
    }

    public String getId() {
        return id;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public OfflinePlayer getOwnerOffline() {
        return Bukkit.getOfflinePlayer(getOwnerUUID());
    }

    public Optional<Player> getOwner() {
        return Optional.ofNullable(Bukkit.getPlayer(getOwnerUUID()));
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public CinematicKeyframe[] getKeyframes() {
        return Arrays.copyOf(keyframes, keyframes.length);
    }

    public CinematicKeyframe getKeyframe(int index) {
        return (index >= 0 && index < keyframes.length) ? keyframes[index] : null;
    }

    public CinematicKeyframe getFirstKeyframe() {
        return getKeyframe(0);
    }

    public CinematicKeyframe getLastKeyframe() {
        return getKeyframe(getKeyframeCount() - 1);
    }

    public int getKeyframeCount() {
        return keyframes.length;
    }

    public CinematicCompletion getCompletion() {
        return completion;
    }

    public boolean isPlayerFreelookAllowed() {
        return playerFreelookAllowed;
    }

    @Override
    public CinematicPath clone() {
        return new CinematicPath(this);
    }

    @Override
    public Iterator<CinematicKeyframe> iterator() {
        return Iterators.forArray(keyframes);
    }

    public static Builder builder(CinematicPath path) {
        return new Builder(path);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {

        private String id;
        private UUID ownerUUID;
        private LocalDateTime creationDate;

        private List<CinematicKeyframe> keyframes;
        private CinematicCompletion completion = CinematicCompletion.TELEPORT_BACK;

        private boolean playerFreelookAllowed = false;

        private Builder(CinematicPath path) {
            this.id = path.getId();
            this.ownerUUID = path.getOwnerUUID();
            this.creationDate = path.getCreationDate();

            this.keyframes = Lists.newArrayList(path.keyframes); // For mutability
            this.completion = path.getCompletion();

            this.playerFreelookAllowed = path.isPlayerFreelookAllowed();
        }

        private Builder(String id) {
            this.id = id;
            this.creationDate = LocalDateTime.now();
            this.keyframes = new ArrayList<>();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder owner(UUID ownerUUID) {
            this.ownerUUID = ownerUUID;
            return this;
        }

        public Builder owner(OfflinePlayer player) {
            return owner(player.getUniqueId());
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder keyframe(CinematicKeyframe keyframe) {
            this.keyframes.add(keyframe);
            return this;
        }

        public Builder keyframe(int index, CinematicKeyframe keyframe) {
            this.keyframes.add(index, keyframe);
            return this;
        }

        public Builder removeKeyframe(int index) {
            this.keyframes.remove(index);
            return this;
        }

        public Builder clearKeyframes() {
            this.keyframes.clear();
            return this;
        }

        public Builder keyframes(CinematicKeyframe... keyframes) {
            Collections.addAll(this.keyframes, keyframes);
            return this;
        }

        public Builder completion(CinematicCompletion completion) {
            this.completion = completion;
            return this;
        }

        public Builder playerFreelook(boolean allowed) {
            this.playerFreelookAllowed = allowed;
            return this;
        }

        public CinematicPath build() {
            return new CinematicPath(id, ownerUUID, creationDate, keyframes.toArray(CinematicKeyframe[]::new), completion, playerFreelookAllowed);
        }

    }

    private static final class Serializer implements JsonSerializer<CinematicPath>, JsonDeserializer<CinematicPath> {

        @Override
        public JsonElement serialize(CinematicPath path, Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            object.addProperty("id", path.id);

            if (path.ownerUUID != null) {
                object.addProperty("ownerUUID", path.ownerUUID.toString());
            }

            object.addProperty("creationDate", path.creationDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            object.addProperty("completion", path.completion.name());
            object.addProperty("freelookAllowed", path.playerFreelookAllowed);

            // Keyframes
            JsonArray keyframesArray = new JsonArray();

            for (CinematicKeyframe keyframe : path.keyframes) {
                JsonElement keyframeElement = context.serialize(keyframe);
                if (keyframeElement.isJsonObject()) {
                    continue;
                }

                keyframesArray.add(keyframeElement);
            }

            object.add("keyframes", keyframesArray);

            return object;
        }

        @Override
        public CinematicPath deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (!element.isJsonObject()) {
                throw new JsonParseException("Expected JsonObject (root), got " + element.getClass().getSimpleName());
            }

            JsonObject object = element.getAsJsonObject();

            String id = object.get("id").getAsString();
            UUID ownerUUID = object.has("ownerUUID") ? UUID.fromString(object.get("ownerUUID").getAsString()) : null;
            LocalDateTime creationDate = LocalDateTime.parse(object.get("creationDate").getAsString());

            CinematicCompletion completion = Enums.getIfPresent(CinematicCompletion.class, object.get("completion").getAsString()).or(CinematicCompletion.TELEPORT_BACK);
            boolean freelookAllowed = object.get("freelookAllowed").getAsBoolean();

            JsonArray keyframesArray = object.getAsJsonArray("keyframes");
            CinematicKeyframe[] keyframes = new CinematicKeyframe[keyframesArray.size()];

            for (int i = 0; i < keyframes.length; i++) {
                JsonElement keyframeElement = keyframesArray.get(i);
                if (!keyframeElement.isJsonObject()) {
                    throw new JsonParseException("Expected JsonObject (keyframe index " + i + "), got " + element.getClass().getSimpleName());
                }

                JsonObject keyframeObject = keyframeElement.getAsJsonObject();
                String keyframeTypeId = keyframeObject.get("type").getAsString();
                Type keyframeType = AbstractKeyframe.getSerializableType(keyframeTypeId);

                if (keyframeType == null) {
                    throw new JsonParseException("Unexpected keyframe type (keyframe index " + i + "), got \"" + keyframeTypeId + "\"");
                }

                keyframes[i] = context.deserialize(keyframeObject, type);
            }

            return CinematicPath.builder(id)
                    .owner(ownerUUID)
                    .creationDate(creationDate)
                    .completion(completion)
                    .playerFreelook(freelookAllowed)
                    .keyframes(keyframes)
                    .build();
        }

    }

}
