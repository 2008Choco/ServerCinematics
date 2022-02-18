package optic_fusion1.servercinematics.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import optic_fusion1.servercinematics.cinematic.CinematicPath;

public class PathPlaybackStoppedEvent extends PathEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private StopCause cause;

    public PathPlaybackStoppedEvent(StopCause cause, Player player, CinematicPath path, long id) {
        super(player, path, id);
        this.cause = cause;
    }

    public StopCause getCause() {
        return cause;
    }

    public static HandlerList getHanderList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public enum StopCause {
        FINISHED, LEFT, MANUAL, FSTOP, PLUGIN;
    }
}
