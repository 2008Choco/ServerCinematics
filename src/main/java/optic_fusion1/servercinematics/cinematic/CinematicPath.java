package optic_fusion1.servercinematics.cinematic;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CinematicPath implements Cloneable, Iterable<CinematicKeyframe> {

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

}
