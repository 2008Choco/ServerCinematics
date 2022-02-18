package email.com.gmail.cosmoconsole.bukkit.camera;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

/**
 * Represents a ServerCinematics waypoint.
 */
public class CinematicWaypoint {
    private Location loc;
    private double speed;
    private double yaw;
    private double pitch;
    private String message = "";
    private List<String> commands = new ArrayList<>();
    private double delay = 0;
    private boolean isInstant;
    
    /**
     * Creates a waypoint with only a location.
     * 
     * @param loc The location of the waypoint.
     */
    public CinematicWaypoint(Location loc) {
        this(loc, -1, 444, 444);
    }

    /**
     * Creates a waypoint with location, speed, yaw and pitch.
     * 
     * @param loc The location of the waypoint.
     * @param speed The speed at the waypoint.
     * @param yaw The yaw of the camera at the waypoint.
     * @param pitch The pitch of the camera at the waypoint.
     */
    public CinematicWaypoint(Location loc, double speed, double yaw, double pitch) {
        if (loc == null) {
            throw new IllegalArgumentException("location of the waypoint cannot be null");
        }
        this.loc = loc;
        setSpeed(speed);
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public Location getLocation() {
        return loc;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    public double getYaw() {
        return yaw;
    }
    
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }
    
    public double getPitch() {
        return pitch;
    }
    
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public double getDelay() {
        return delay;
    }
    
    public void setDelay(double delay) {
        this.delay = Math.max(0, delay);
    }
    
    public boolean isInstant() {
        return isInstant;
    }
    
    public void setIsInstant(boolean instant) {
        isInstant = instant;
    }
}
