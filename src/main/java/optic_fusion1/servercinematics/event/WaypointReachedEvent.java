package optic_fusion1.servercinematics.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WaypointReachedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String path;
    private final int waypointId;
    private final int waypointLen;

    public WaypointReachedEvent(Player player, String path, int index, int length) {
        this.player = player;
        this.path = path;
        this.waypointId = index;
        this.waypointLen = length;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getPathName() {
        return this.path;
    }

    public int getWaypointIndex() {
        return this.waypointId;
    }

    public int getWaypointLength() {
        return this.waypointLen;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
