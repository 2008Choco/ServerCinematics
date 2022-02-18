package optic_fusion1.servercinematics.user;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class User {

    private UUID uniqueID;
    private boolean isPlaying;
    private String pathName;
    private boolean isPathless;
    private boolean isInTpMode;

    public User(UUID uniqueID) {
        this.uniqueID = uniqueID;
    }

    public UUID getUniqueID() {
        return uniqueID;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uniqueID);
    }

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String name) {
        this.pathName = name;
    }

    public boolean isPathless() {
        return isPathless;
    }

    public void setIsPathless(boolean isPathless) {
        this.isPathless = isPathless;
    }

    public boolean isInTpMode() {
        return isInTpMode;
    }

    public void setIsInTpMode(boolean isInTpMode) {
        this.isInTpMode = isInTpMode;
    }
}
