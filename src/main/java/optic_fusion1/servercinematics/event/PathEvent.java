package optic_fusion1.servercinematics.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private Player player;
    private String path;
    private long id;

    public PathEvent(Player player, String path, long id) {
        this.player = player;
        this.path = path;
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPath() {
        return path;
    }

    public long getId() {
        return id;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
