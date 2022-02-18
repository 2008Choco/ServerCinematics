package email.com.gmail.cosmoconsole.bukkit.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

public class SavedPlayerPath {

    private UUID owner;
    private long date;
    private Boolean teleport;
    private Boolean pathless;
    private Double speed;
    private String pathnames;
    private List<Location> waypoints;
    private List<Double> waypoints_s;
    private List<Double> waypoints_y;
    private List<Double> waypoints_p;
    private List<String> waypoints_m;
    private List<List<String>> waypoints_c;
    private List<Integer> waypoints_l;
    private List<Double> waypoints_d;
    private List<Boolean> waypoints_i;
    private int waypoints_f;
    private double waypoints_t;
    
    SavedPlayerPath(UUID o, boolean t, boolean p, double s, String n,
                    List<Location> w, List<Double> w_s, List<Double> w_y,
                    List<Double> w_p, List<String> w_m, List<List<String>> w_c,
                    List<Integer> w_l, List<Double> w_d, List<Boolean> w_i,
            int w_f, double w_t) {
        owner = o;
        date = System.currentTimeMillis();
        teleport = t;
        pathless = p;
        speed = s;
        pathnames = n;
        waypoints = new ArrayList<>(w);
        waypoints_s = new ArrayList<>(w_s);
        waypoints_y = new ArrayList<>(w_y);
        waypoints_p = new ArrayList<>(w_p);
        waypoints_m = new ArrayList<>(w_m);
        waypoints_c = new ArrayList<>(w_c);
        waypoints_l = new ArrayList<>(w_l);
        waypoints_d = new ArrayList<>(w_d);
        waypoints_i = new ArrayList<>(w_i);
        waypoints_f = w_f;
        waypoints_t = w_t;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public long getSavedAt() {
        return date;
    }

    public long getDate() {
        return date;
    }

    public Boolean getTeleport() {
        return teleport;
    }

    public Boolean getPathless() {
        return pathless;
    }

    public Double getSpeed() {
        return speed;
    }

    public String getPathnames() {
        return pathnames;
    }

    public List<Location> getWaypoints() {
        return waypoints;
    }

    public List<Double> getWaypoints_s() {
        return waypoints_s;
    }

    public List<Double> getWaypoints_y() {
        return waypoints_y;
    }

    public List<Double> getWaypoints_p() {
        return waypoints_p;
    }

    public List<String> getWaypoints_m() {
        return waypoints_m;
    }

    public List<List<String>> getWaypoints_c() {
        return waypoints_c;
    }

    public List<Integer> getWaypoints_l() {
        return waypoints_l;
    }

    public List<Double> getWaypoints_d() {
        return waypoints_d;
    }

    public List<Boolean> getWaypoints_i() {
        return waypoints_i;
    }

    public int getWaypoints_f() {
        return waypoints_f;
    }

    public double getWaypoints_t() {
        return waypoints_t;
    }
}
