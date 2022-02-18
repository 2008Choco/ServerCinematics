package optic_fusion1.servercinematics.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PathPlaybackStartedEvent extends PathEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private StartCause cause;

    public PathPlaybackStartedEvent(StartCause cause, Player player, String path, long id) {
        super(player, path, id);
        this.cause = cause;
    }

    public StartCause getCause() {
        return cause;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public enum StartCause {
        MANUAL, PLAYLIST, FPLAY, PLUGIN;
    }
}
