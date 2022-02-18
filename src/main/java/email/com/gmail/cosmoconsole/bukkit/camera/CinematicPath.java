package email.com.gmail.cosmoconsole.bukkit.camera;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a ServerCinematics path.
 */
public class CinematicPath {
    private List<CinematicWaypoint> waypoints = new ArrayList<>();
    private boolean shouldTeleportToStartAfterPlayback;
    private boolean shouldTeleportBackAfterPlayback;
    private boolean canPlayerTurnCameraDuringDelay;

    public List<CinematicWaypoint> getWaypoints() {
        return waypoints;
    }
    
    public void addWaypoint(CinematicWaypoint waypoint) {
        waypoints.add(waypoint);
    }
    
    public boolean shouldTeleportToStartAfterPlayback() {
        return shouldTeleportToStartAfterPlayback;
    }
    
    public void setShouldTeleportToStartAfterPlayback(boolean state) {
        shouldTeleportToStartAfterPlayback = state;
    }
    
    public boolean shouldTeleportBackAfterPlayback() {
        return shouldTeleportBackAfterPlayback;
    }
    
    public void setShouldTeleportBackAfterPlayback(boolean state) {
        shouldTeleportBackAfterPlayback = state;
    }
    
    public boolean canPlayerTurnCameraDuringDelay() {
        return canPlayerTurnCameraDuringDelay;
    }
    
    public void setCanPlayerTurnCameraDuringDelay(boolean state) {
        canPlayerTurnCameraDuringDelay = state;
    }
}
