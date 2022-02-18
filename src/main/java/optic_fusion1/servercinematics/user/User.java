package optic_fusion1.servercinematics.user;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import optic_fusion1.servercinematics.cinematic.CinematicPath;

public class User {

    private UUID uniqueID;
    private boolean isPlaying;
    private CinematicPath currentPath;
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

    public void setCurrentPath(CinematicPath currentPath) {
        this.currentPath = currentPath;
    }

    public CinematicPath getCurrentPath() {
        return currentPath;
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
