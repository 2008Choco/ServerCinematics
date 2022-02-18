package email.com.gmail.cosmoconsole.bukkit.camera;

import optic_fusion1.servercinematics.command.CameraCommand;
import optic_fusion1.servercinematics.event.PathPlaybackStartedEvent;
import optic_fusion1.servercinematics.event.PathPlaybackStoppedEvent;
import optic_fusion1.servercinematics.event.PathPlaybackStoppedEvent.StopCause;
import optic_fusion1.servercinematics.event.PathPlaybackStartedEvent.StartCause;
import optic_fusion1.servercinematics.event.WaypointReachedEvent;
import optic_fusion1.servercinematics.listener.PlayerListener;
import optic_fusion1.servercinematics.user.User;
import optic_fusion1.servercinematics.user.UserManager;
import optic_fusion1.servercinematics.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ServerCinematics main class.
 */
public class ServerCinematics extends JavaPlugin implements Listener, TabCompleter {
    /**
     * A player's path is MODIFIED_PATH if the player has modified it and it hasn't been saved since.
     */
    public final String MODIFIED_PATH = "";
    private static final double SPDLIMIT = 0.5;
    private static final double CAN_TURN = 2.5;
    private static final double DEFSPEED = 5.0;
    private static final int CALCPOINTS = 20;
    private static long pbid = 0;
    private File paths;
    private boolean shortPrefix = false;
    private boolean finalWaypointTeleport = true;
    // player info
    private static final HashMap<UUID, Boolean> teleport = new HashMap<>();
    private static final HashMap<UUID, Boolean> pathless = new HashMap<>();
    private static final HashMap<UUID, Double> speed = new HashMap<>();
    private static final HashMap<UUID, String> pathnames = new HashMap<>();
    // path info
    private static final HashMap<UUID, List<Location>> waypoints = new HashMap<>(); // location
    private static final HashMap<UUID, List<Double>> waypoints_s = new HashMap<>(); // speed
    private static final HashMap<UUID, List<Double>> waypoints_y = new HashMap<>(); // yaw
    private static final HashMap<UUID, List<Double>> waypoints_p = new HashMap<>(); // pitch
    private static final HashMap<UUID, List<String>> waypoints_m = new HashMap<>(); // message
    private static final HashMap<UUID, List<List<String>>> waypoints_c = new HashMap<>(); // commands
    private static final HashMap<UUID, List<Integer>> waypointOptions = new HashMap<>(); // waypoint flags
    private static final HashMap<UUID, List<Double>> waypointDelays = new HashMap<>(); // delay
    private static final HashMap<UUID, List<Boolean>> waypoints_i = new HashMap<>(); // instant?
    private static final HashMap<UUID, Integer> waypoints_f = new HashMap<>(); // path flags
    private static final HashMap<UUID, Double> waypoints_t = new HashMap<>(); // time to play path, or -1 if not yet determined
    // playlist info
    private static final HashMap<UUID, List<String>> pl_paths = new HashMap<>();
    private static final HashMap<UUID, Boolean> pl_playing = new HashMap<>();
    private static final HashMap<UUID, Boolean> pl_looping = new HashMap<>();
    private static final HashMap<UUID, Integer> pl_index = new HashMap<>();
    private static final HashMap<UUID, Double> multipl = new HashMap<>();
    private int timer_id = 0;
    // temp info
    private static final HashMap<UUID, Double> speed_a = new HashMap<>();
    private static final HashMap<UUID, Boolean> old_af = new HashMap<>();
    private static final HashMap<UUID, Boolean> old_f = new HashMap<>();
    private static final HashMap<UUID, GameMode> old_gm = new HashMap<>();
    private static final HashMap<UUID, Float> old_fsp = new HashMap<>();
    private static final HashMap<UUID, Boolean> playing = new HashMap<>();
    private static final HashMap<UUID, Boolean> paused = new HashMap<>();
    // interpolated path info (temp)
    private static final HashMap<UUID, List<Location>> wx = new HashMap<>(); // location
    private static final HashMap<UUID, List<Double>> wxs = new HashMap<>(); // speed
    private static final HashMap<UUID, List<Double>> wxy = new HashMap<>(); // yaw
    private static final HashMap<UUID, List<Double>> wxp = new HashMap<>(); // pitch
    private static final HashMap<UUID, List<String>> wxm = new HashMap<>(); // message
    private static final HashMap<UUID, List<List<String>>> wxc = new HashMap<>(); // commands
    private static final HashMap<UUID, List<Integer>> wxl = new HashMap<>(); // waypoint flags
    private static final HashMap<UUID, List<Double>> wxd = new HashMap<>(); // delay
    private static final HashMap<UUID, List<Boolean>> wxi = new HashMap<>(); // instant?
    private static final HashMap<UUID, List<Boolean>> wxtemp = new HashMap<>(); // is point interpolated?
    private static final HashMap<UUID, List<Integer>> wxindx = new HashMap<>(); // actual index of original point
    private static final HashMap<UUID, Integer> wxf = new HashMap<>();
    private static final HashMap<UUID, Boolean> wm = new HashMap<>();
    private static final HashMap<UUID, Location> old_loc = new HashMap<>();
    private static final HashMap<UUID, Integer> timer_ids = new HashMap<>();
    private static final HashMap<UUID, Long> pbids = new HashMap<>();
    private static final List<Player> TEMP_JOINS = new ArrayList<>();

    private static final UserManager USER_MANAGER = new UserManager();
    private static final PluginManager PLUGIN_MANAGER = Bukkit.getPluginManager();
    private boolean isInGlobalMode;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerListeners();
        createFolders();
        initializeVariables();
        registerCommand();
    }

    private void initializeVariables() {
        this.shortPrefix = getConfig().getBoolean("short-prefix", false);
        this.finalWaypointTeleport = getConfig().getBoolean("final-waypoint-teleport", true);
    }

    private void createFolders() {
        File dataFolder = getDataFolder();
        if (!dataFolder.isDirectory()) {
            dataFolder.mkdir();
        }
        File paths = new File(dataFolder, "paths");
        if (!paths.isDirectory()) {
            paths.mkdir();
        }
        this.paths = paths;
    }

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand("camera");
        CameraCommand cameraCommand = new CameraCommand(this);
        pluginCommand.setExecutor(cameraCommand);
        pluginCommand.setTabCompleter(cameraCommand);
    }

    private void registerListeners() {
        registerListener(new PlayerListener(this));
        PLUGIN_MANAGER.registerEvents(this, this);
    }

    private void registerListener(Listener listener) {
        PLUGIN_MANAGER.registerEvents(listener, this);
    }
    /**
     * Loads and starts to play a path for a specific player. If the player has a path they are editing, the path will be overwritten.
     * If you want to avoid this, you can store the current player path for later by using savePlayerPath().
     * If already playing a path, throws an exception.
     * 
     * @param player
     * @param path
     * @param speed The speed to play at.
     * @param tpmode Whether to enable tpmode for the player.
     * @param pathless Whether to enable pathless mode for the player.
     * @return An ID for this playback instance that is also available on the start/stop events. Always positive; 0 if playback could not start.
     */
    public long startPath(Player player, String path, double speed, boolean tpmode, boolean pathless) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        User user = getUserManager().getUser(player.getUniqueId());
        if (user.isPlaying()) {
            throw new IllegalArgumentException("player already playing");
        }
        int j = loadForPlaylist(player, path, false);
        if (j == -2) {
            throw new IllegalArgumentException("No path called '" + path + "' exists");
        } else if (j == -1) {
            throw new Exception("An unknown exception occurred while loading path: it has been printed to server console");
        } else if (j > 0) {
            pl_playing.put(player.getUniqueId(), false);
            pl_looping.put(player.getUniqueId(), false);
            this.speed.put(player.getUniqueId(), speed);
            teleport.put(player.getUniqueId(), tpmode);
            this.pathless.put(player.getUniqueId(), pathless);
            return play(player, null, true, StartCause.PLUGIN);
        }
        return 0;
    }
    
    /**
     * Starts to play a path for a specific player. If the player has a path they are editing, the path will be overwritten.
     * If you want to avoid this, you can store the current player path for later by using savePlayerPath().
     * If already playing a path, throws an exception.
     * 
     * @param player
     * @param path The path as a CinematicPath.
     * @param speed The speed to play at.
     * @param tpmode Whether to enable tpmode for the player.
     * @param pathless Whether to enable pathless mode for the player.
     * @return An ID for this playback instance that is also available on the start/stop events. Always positive; 0 if playback could not start.
     */
    public long startPath(Player player, CinematicPath path, double speed, boolean tpmode, boolean pathless) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        User user = getUserManager().getUser(player.getUniqueId());
        if (user.isPlaying()) {
            throw new IllegalArgumentException("player already playing");
        }
        deserializePath(player, path);
        if (this.waypoints.get(player.getUniqueId()).size() < 1) {
            return 0;
        }
        teleport.put(player.getUniqueId(), tpmode);
        this.pathless.put(player.getUniqueId(), pathless);
        this.speed.put(player.getUniqueId(), speed);
        pl_playing.put(player.getUniqueId(), false);
        pl_looping.put(player.getUniqueId(), false);
        return play(player, null, true, StartCause.PLUGIN);
    }
    
    @Deprecated
    public long startPath(Player player, String path, boolean tpmode, boolean pathless) throws Exception {
        return startPath(player, path, speed.get(player.getUniqueId()), tpmode, pathless);
    }

    @Deprecated
    public long startPath(Player player, CinematicPath path, boolean tpmode, boolean pathless) {
        return startPath(player, path, speed.get(player.getUniqueId()), tpmode, pathless);
    }
    
    /**
     * Saves the current path used by the player. Designed to only be used around paths played by the plugin if it isn't obvious to the player that they should save first. Needs to be called before playing paths.
     * 
     * @param player The player to save the path of.
     * @return A SavedPlayerPath that can be given to restorePlayerPath().
     */
    public SavedPlayerPath savePlayerPath(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        
        UUID u = player.getUniqueId();
        return new SavedPlayerPath(u,
                                   teleport.get(u),
                                   pathless.get(u),
                                   speed.get(u),
                                   pathnames.get(u),
                                   waypoints.get(u),
                                   waypoints_s.get(u),
                                   waypoints_y.get(u),
                                   waypoints_p.get(u),
                                   waypoints_m.get(u),
                                   waypoints_c.get(u),
                                   waypointOptions.get(u),
                                   waypointDelays.get(u),
                                   waypoints_i.get(u),
                                   waypoints_f.get(u),
                                   waypoints_t.get(u));
    }
    
    /**
     * Restores a previously saved path by the player. Designed to only be used around paths played by the plugin if it isn't obvious to the player that they should save first.
     * 
     * @param player The player to restore the path to.
     * @param path A SavedPlayerPath that is to be restored.
     */
    public void restorePlayerPath(Player player, SavedPlayerPath path) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        
        UUID pathOwnerUUID = path.getOwner();
        if (player.getUniqueId() != pathOwnerUUID) {
            throw new IllegalArgumentException("Trying to restore path to different player");
        }
        putIfNotNull(teleport, pathOwnerUUID, path.getTeleport());
        putIfNotNull(pathless, pathOwnerUUID, path.getPathless());
        putIfNotNull(speed, pathOwnerUUID, path.getSpeed());
        putIfNotNull(pathnames, pathOwnerUUID, path.getPathnames());
        putIfNotNull(waypoints, pathOwnerUUID, path.getWaypoints());
        putIfNotNull(waypoints_s, pathOwnerUUID, path.getWaypoints_s());
        putIfNotNull(waypoints_y, pathOwnerUUID, path.getWaypoints_y());
        putIfNotNull(waypoints_p, pathOwnerUUID, path.getWaypoints_p());
        putIfNotNull(waypoints_m, pathOwnerUUID, path.getWaypoints_m());
        putIfNotNull(waypoints_c, pathOwnerUUID, path.getWaypoints_c());
        putIfNotNull(waypointOptions, pathOwnerUUID, path.getWaypoints_l());
        putIfNotNull(waypointDelays, pathOwnerUUID, path.getWaypoints_d());
        putIfNotNull(waypoints_i, pathOwnerUUID, path.getWaypoints_i());
        putIfNotNull(waypoints_f, pathOwnerUUID, path.getWaypoints_f());
        putIfNotNull(waypoints_t, pathOwnerUUID, path.getWaypoints_t());
        clearCache(player);
    }

    /**
     * Pauses the playback for a specific player.
     * 
     * @param player The player whose playback is to be paused.
     */
    public void pausePath(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (isTrue(playing.get(player.getUniqueId())) && isFalse(paused.get(player.getUniqueId()))) {
            paused.put(player.getUniqueId(), true);
        }
    }
    
    /**
     * Resumes the playback for a specific player.
     * 
     * @param player The player whose playback is to be resumed from a pause.
     */
    public void resumePath(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (isTrue(playing.get(player.getUniqueId())) && isTrue(paused.get(player.getUniqueId()))) {
            paused.put(player.getUniqueId(), false);
        }
    }
    
    /**
     * Stops playing the path for a player.
     * 
     * @param player
     */
    public void stopPath(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        stop(player, StopCause.PLUGIN);
    }

    private <K, V> void putIfNotNull(HashMap<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        } else {
            map.remove(key);
        }
    }

    private void stopGlobal() {
        if (!isInGlobalMode) {
            return;
        }
        isInGlobalMode = false;
        for (Player ps: getServer().getOnlinePlayers()) {
            Location l = getServer().getWorlds().get(0).getSpawnLocation();
            ps.teleport(l);
            GameMode gm = getServer().getDefaultGameMode();
            ps.setGameMode(gm);
            ps.setAllowFlight(gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR);
            ps.setFlying(gm == GameMode.SPECTATOR);
            ps.setFlySpeed(1.0f);
        }
    }

    private long play(final Player player, final CommandSender commandSender, final boolean is_fplay, final StartCause cause) {
        final UUID uniqueId = player.getUniqueId();
        return playWithFakeUUID(player, commandSender, uniqueId, is_fplay, cause);
    }
    
    // not necessarily fake UUID despite the misleading name
    private long playWithFakeUUID(final Player player, final CommandSender commandSender, final UUID uniqueId, final boolean is_fplay, final StartCause cause) {
        if (getSafeWaypoints(uniqueId).size() <= 0 && commandSender != null) {
            commandSender.sendMessage(Utils.colorize((shortPrefix ? "&7[&7&l[**]<|&7]" : "&7[&8&l[**]<|&eServer&6Cinematics&7]") + "&cThe path seems to have no waypoints!"));
            return 0;
        }
        long id = ++pbid;
        if (id <= 0) {
            id = 0;
        }
        pbids.put(uniqueId, id);
        if (!isInGlobalMode) {
            this.old_af.put(uniqueId, player.getAllowFlight());
            this.old_f.put(uniqueId, player.isFlying());
            this.old_gm.put(uniqueId, player.getGameMode());
            this.old_loc.put(uniqueId, player.getLocation());
            this.old_fsp.put(uniqueId, player.getFlySpeed());
            player.setFlySpeed(0.0f);
        }
        this.playing.put(uniqueId, true);
        final int tid = this.timer_id++;
        this.timer_ids.put(uniqueId, tid);
        if (!this.speed.containsKey(uniqueId)) {
            this.speed.put(uniqueId, 5.0);
        }
        this.speed_a.put(uniqueId, this.speed.get(uniqueId));
        User user = getUserManager().getUser(player.getUniqueId());
        PathPlaybackStartedEvent evt = new PathPlaybackStartedEvent(cause, player, user.getPathName(), id);
        getServer().getPluginManager().callEvent(evt);
        new BukkitRunnable() {
            CommandSender owner = commandSender;
            Player p = player;
            UUID u = uniqueId;
            // path info: locations, speeds, yaws, pitches, messages, flags, delays, instant toggles
            List<Location> w = new ArrayList<Location>(getSafeWaypoints(this.u));
            List<Double> ws = new ArrayList<Double>(getSafeWaypointSpeeds(this.u));
            List<Double> wy = new ArrayList<Double>(getSafeWaypointYaw(this.u));
            List<Double> wp = new ArrayList<Double>(getSafeWaypointPitch(this.u));
            List<String> wmsg = new ArrayList<String>(getSafeWaypointMessages(this.u));
            List<Integer> wl = new ArrayList<Integer>(waypointOptions.get(this.u));
            List<Double> wd = new ArrayList<Double>(waypointDelays.get(this.u));
            List<Boolean> wi = new ArrayList<Boolean>(waypoints_i.get(this.u));
            List<Integer> windx = new ArrayList<Integer>();
            List<Boolean> wtemp = new ArrayList<Boolean>();
            // path flags
            int wf = getSafeWaypointFlags(this.u);
            // waypoint commands
            List<List<String>> wcmd = new ArrayList<List<String>>(getSafeWaypointCommands(this.u));
            // current waypoint index
            int i = 0;
            // the ID of this path task
            int id = tid;
            // distance to plane formed by next waypoint
            double ls = 0.0;
            //double ld = 0.0;
            double xm = 0.0;
            double ym = 0.0;
            double zm = 0.0;
            double op = 0.0;
            double oy = 0.0;
            double fp = 0.0;
            double fy = 0.0;
            // distance to next waypoint from last waypoint
            double dist = 0.0;
            // whether waypoint has a proper yaw value
            boolean yaw = false;
            // whether waypoint has a proper pitch value
            boolean pitch = false;
            // should end playback instantly
            boolean end = false;
            // teleport mode?
            boolean tm = false;
            // pathless mode?
            boolean nopath = false;
            // running for the first time
            boolean first = true;
            boolean fplay = is_fplay;
            // should this waypoint be shown in instant mode?
            boolean npoint = false;
            // usually [0, 1], approaches 1 as we approach next waypoint
            double q = 0.0;
            // speed multiplier for (f)tplay
            double M = 1.0;
            long ticks_moved = 0;
            // effective position, if not in tpmode
            Location effpos = null;
            // array of waypoints to use in instant mode
            ArrayList<Integer> apoints = new ArrayList<Integer>();
            int totaldelay = 0;
            boolean initAsGlobal = isInGlobalMode;
            
            private int getCurrentId() {
                return timer_ids.get(this.u);
            }
            
            private boolean isCurrent() {
                return this.id == this.getCurrentId();
            }
/*
            private String debugFmt(Location l) {
                return String.format("(%+7.3f,%+7.3f,%+7.3f)", l.getX(), l.getY(), l.getZ());
            }
            private String debugFmt(Vector l) {
                return String.format("(%+7.3f,%+7.3f,%+7.3f)", l.getX(), l.getY(), l.getZ());
            }
            */
            public void run() {
                if (this.w.size() <= 0) {
                    if (!isInGlobalMode) {
                        p.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cThe path seems to have no waypoints!")));
                    }
                    this.cancel();
                    if (!isInGlobalMode) {
                        stop(this.p, PathPlaybackStoppedEvent.StopCause.FINISHED);
                    } else {
                        stopGlobalUUID(u);
                    }
                    return;
                }
                if (this.first) {
                    this.p.setGameMode(GameMode.SPECTATOR);
                    this.p.setAllowFlight(true);
                    this.p.setFlying(true);
                    if (!isInGlobalMode) {
                        this.u = this.p.getUniqueId();
                    } else {
                        for (Player ps: getServer().getOnlinePlayers()) {
                            ps.setGameMode(GameMode.SPECTATOR);
                            ps.setAllowFlight(true);
                            ps.setFlying(true);
                            ps.setFlySpeed(0.0f);
                        }
                    }
                    if (multipl.containsKey(u)) {
                        M = multipl.remove(u);
                    }
                    this.tm = (teleport.containsKey(this.u) && teleport.get(this.u));
                    this.nopath = (pathless.containsKey(this.u) && pathless.get(this.u));
                    waypointEvent(0);
                    
                    // interpolate paths
                    // creates a whole bunch of intermediate points that are never visible to the player
                    
                    if (isInGlobalMode || !hasCache(this.p) || nopath) {
                        if (pl_playing.get(u) && owner != null) {
                            owner.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &eCalculating spline, please wait...")));
                        }
                        new BukkitRunnable() {
                            List<Location> ow = new ArrayList<Location>(w);
                            List<Double> ows = new ArrayList<Double>(ws);
                            List<Double> owy = new ArrayList<Double>(wy);
                            List<Double> owp = new ArrayList<Double>(wp);

                            List<Location> v = new ArrayList<Location>(ow);
                            List<Double> vs = new ArrayList<Double>(ows);
                            List<Double> vy = new ArrayList<Double>(owy);
                            List<Double> vp = new ArrayList<Double>(owp);

                            List<String> vm = new ArrayList<String>(wmsg);
                            List<List<String>> vc = new ArrayList<List<String>>(wcmd);
                            List<Double> vd = new ArrayList<Double>(wd);
                            List<Integer> vl = new ArrayList<Integer>(wl);
                            List<Boolean> vi = new ArrayList<Boolean>(wi);

                            List<Integer> vindx = new ArrayList<Integer>();
                            List<Boolean> vtemp = new ArrayList<Boolean>();
                            int size = this.ow.size() - 1;
                            int[] lsz = new int[this.size];
                            public void run() {
                                for (int i = 0; i <= size; ++i) {
                                    this.vindx.add(i);
                                }
                                apoints.add(0);
                                for (int i = 0; i < this.ow.size(); i++) {
                                    vtemp.add(false);
                                }
                                for (int n = 0; n < this.size; ++n) {
                                    if (!isCurrent()) {
                                        this.cancel();
                                        return;
                                    }
                                    final int min = Math.min(20, (int)((this.wget(n).distance(this.wget(n + 1)) - 1.0) * 2.0));
                                    final boolean isInstant = tm && this.vi.get(n); 
                                    this.lsz[n] = min;
                                    
                                    final double prevSpeed = this.ows.get(n);
                                    double prevYaw = this.owy.get(n);
                                    double prevPitch = this.owp.get(n);
                                    
                                    final double nextSpeed = this.ows.get(n + 1);
                                    final double nextYaw = this.owy.get(n + 1);
                                    final double nextPitch = this.owp.get(n + 1);
                                    
                                    if (improper(prevYaw) && n == 0) {
                                        prevYaw = this.ow.get(0).getYaw();
                                    }
                                    if (improper(prevPitch) && n == 0) {
                                        prevPitch = this.ow.get(0).getPitch();
                                    }
                                    int max = -1;
                                    for (int j = 1; j < min; ++j) {
                                        final double n2 = Double.valueOf(j) / min;
                                        final int n3 = this.maxIndex(n) + j;
                                        // interpolated location
                                        Location loc = ServerCinematics.catmull_rom_3d(n2, this.wget(n - 1), this.wget(n), this.wget(n + 1), this.wget(n + 2));
                                        this.v.add(n3, loc);
                                        // interpolated speed
                                        this.vs.add(n3, (prevSpeed == -1.0 || nextSpeed == -1.0) ? -1.0 : this.linear(prevSpeed, nextSpeed, n2));
                                        // interpolated yaw
                                        this.vy.add(n3, (improper(prevYaw) || improper(nextYaw)) ? 444.0
                                                : formatAngleYaw(this.linearO(properYaw(prevYaw), properYaw(nextYaw), n2)));
                                        // interpolated pitch
                                        this.vp.add(n3, (improper(prevPitch) || improper(nextPitch)) ? 444.0
                                                : this.linear(prevPitch, nextPitch, n2));
                                        // other info is always default
                                        this.vm.add(n3, "");
                                        this.vc.add(n3, new ArrayList<String>());
                                        this.vd.add(n3, 0.0);
                                        this.vl.add(n3, 0);
                                        this.vi.add(n3, isInstant);
                                        // point is indeed interpolated
                                        this.vtemp.add(n3, true);
                                        // and does not correspond to any original point
                                        this.vindx.add(n3, -1);
                                        max = n3 + 1;
                                    }
                                    if (max >= 0)
                                        apoints.add(max);
                                }
                                
                                // add placeholder blank points
                                if (this.vm.get(0).length() > 0 || this.vc.get(0).size() > 0 || this.vd.get(0) > 0 || (this.vl.get(0)&1)>0) {
                                    for (int i = 0; i < 2; i++) {
                                        this.v.add(0, this.v.get(0));
                                        this.vs.add(0, this.vs.get(0));
                                        this.vy.add(0, this.vy.get(0));
                                        this.vp.add(0, this.vp.get(0));
                                        this.vm.add(0, "");
                                        this.vc.add(0, new ArrayList<String>());
                                        this.vd.add(0, 0.0);
                                        this.vl.add(0, 0);
                                        this.vtemp.add(0, true);
                                        this.vi.add(0, false);
                                        this.vindx.add(0, -1);
                                    }
                                }
                                /*int ti = 0;
                                for (Location l: this.v) {
                                    System.out.println(String.format("%7d ", this.vindx.get(ti)) + debugFmt(l));
                                    ++ti;
                                }
                                for (int i = 0; i < vy.size(); ++i) {
                                    getServer().broadcastMessage("I=" + i + ", O=" + (vindx.get(i) >= 0 ? "Y" : "N") + ", Y=" + String.format("%.4f", vy.get(i)) + ", P=" + String.format("%.4f", vp.get(i)));
                                }*/
                                wx.put(u, this.v);
                                wxs.put(u, this.vs);
                                wxy.put(u, this.vy);
                                wxp.put(u, this.vp);
                                wxm.put(u, this.vm);
                                wxc.put(u, this.vc);
                                wxl.put(u, this.vl);
                                wxd.put(u, this.vd);
                                wxi.put(u, this.vi);
                                wxindx.put(u, this.vindx);
                                wxtemp.put(u, this.vtemp);
                                w = this.v;
                                ws = this.ows;
                                wy = this.owy;
                                wp = this.owp;
                                wmsg = this.vm;
                                wcmd = this.vc;
                                wd = this.vd;
                                wl = this.vl;
                                wtemp = vtemp;
                                wm.put(u, true);
                            }
                            // linear interpolation between two degree angles
                            private double linearO(double a, double b, double c) {
                                return a+optimal(a,b)*c;
                            }/*
                            private double optimal(final double n, final double n2) {
                                return optimalRaw(n + 180, n2 + 180);
                            }
                            private double optimalRaw(final double n, final double n2) {
                                final double n3 = 180.0 - Math.abs(Math.abs(n - n2 + 360.0) - 180.0);
                                if (180.0 - Math.abs(Math.abs(n - n2 + 361.0) - 180.0) > n3) {
                                    return -n3;
                                }
                                return n3;
                            }
                            */
                            private int maxIndex(final int n) {
                                int n2 = 0;
                                for (int i = 0; i < n; ++i) {
                                    n2 += this.lsz[i];
                                }
                                return n2;
                            }
                            
                            // linear interpolation
                            private double linear(final double n, final double n2, final double n3) {
                                return (1.0 - n3) * n + n3 * n2;
                            }
                            
                            private Location wget(final int n) {
                                if (n < 0) {
                                    return this.getNegativePoint(-n);
                                }
                                try {
                                    return this.ow.get(n);
                                }
                                catch (IndexOutOfBoundsException ex) {
                                    return this.ow.get(this.ow.size() - 1);
                                }
                            }
                            
                            // extrapolate a point beyond beginning of path
                            private Location getNegativePoint(final double n) {
                                Location result = cloneLocation(this.ow.get(0)).add(cloneLocation(this.ow.get(1)).subtract(cloneLocation(this.ow.get(0))).getDirection().multiply(n));
                                //System.out.println("neg" + n + " => " + debugFmt(result));
                                return cloneLocation(result);
                            }
                        }.runTaskAsynchronously(ServerCinematics.getPlugin(ServerCinematics.class));
                    }
                    else {
                        this.w = wx.get(this.u);
                        this.ws = wxs.get(this.u);
                        this.wy = wxy.get(this.u);
                        this.wp = wxp.get(this.u);
                        this.wmsg = wxm.get(this.u);
                        this.wcmd = wxc.get(this.u);
                        this.wl = wxl.get(this.u);
                        this.wd = wxd.get(this.u);
                        this.wi = wxi.get(this.u);
                        this.windx = wxindx.get(this.u);
                        this.wtemp = wxtemp.get(this.u);
                    }
                    this.first = false;
                    return;
                }
                if (!this.isCurrent()) {
                    // do not let background task go haywire if it is no longer relevant
                    this.cancel();
                    return;
                }
                if (!isInGlobalMode && !hasCache(this.p)) {
                    return;
                }
                if (isInGlobalMode || hasCache(this.p)) {
                    this.w = wx.get(this.u);
                    this.ws = wxs.get(this.u);
                    this.wy = wxy.get(this.u);
                    this.wp = wxp.get(this.u);
                    this.wmsg = wxm.get(this.u);
                    this.wcmd = wxc.get(this.u);
                    this.wl = wxl.get(this.u);
                    this.wd = wxd.get(this.u);
                    this.wi = wxi.get(this.u);
                    this.windx = wxindx.get(this.u);
                    this.wtemp = wxtemp.get(this.u);
                }
                if (initAsGlobal && !isInGlobalMode) {
                    this.cancel();
                    return;
                }
                if (paused.containsKey(this.u) && paused.get(this.u)) {
                    return;
                }
                if (this.i >= this.w.size() || this.end || this.w.size() < 1) {
                    // reached the end of the path!
                    if (!pl_playing.get(u) && !fplay && owner != null) {
                        owner.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cEnd of path reached.")));
                    }
                    // record new duration of this path
                    waypoints_t.put(this.u, this.ticks_moved * (0.05D * M));
                    // teleport player to final waypoint, to stop them from moving
                    if (this.w.size() > 0 && finalWaypointTeleport) {
                        Location loc = cloneLocation(this.w.get(this.w.size() - 1));
                        if (!this.tm) {
                            Location ploc = this.p.getLocation();
                            loc.setYaw(ploc.getYaw());
                            loc.setPitch(ploc.getPitch());
                        }
                        this.p.teleport(loc);
                    }
                    if (!isInGlobalMode)
                        stop(this.p, false, PathPlaybackStoppedEvent.StopCause.FINISHED);
                    else
                        stopGlobalUUID(this.u);
                    this.cancel();
                    return;
                }
                if (!isInGlobalMode && !hasCache(this.p)) {
                    if (!isInGlobalMode)
                        stop(this.p, PathPlaybackStoppedEvent.StopCause.FINISHED);
                    else
                        stopGlobalUUID(this.u);
                    this.cancel();
                    return;
                }
                if (isFalse(playing.get(this.u))) {
                    this.cancel();
                    return;
                }
                this.npoint = false;
                if (isInGlobalMode && !this.p.isOnline()) {
                    if (getServer().getOnlinePlayers().isEmpty()) return;
                    this.p = getServer().getOnlinePlayers().iterator().next();
                }
                // global mode; all players to gm3
                if (isInGlobalMode)
                    for (Player ps : getServer().getOnlinePlayers()) {
                        ps.setGameMode(GameMode.SPECTATOR);
                    }
                if (!this.tm)
                    effpos = this.p.getLocation();
                if (this.i == 0) { // at first waypoint?
                    final Location location = this.w.get(0);
                    if (!improper(this.wy.get(0))) {
                        location.setYaw((float)(double)this.wy.get(0));
                    }
                    if (!improper(this.wp.get(0))) {
                        location.setPitch((float)(double)this.wp.get(0));
                    }
                    if (isInGlobalMode)
                        for (Player ps : getServer().getOnlinePlayers()) {
                            ps.teleport(location);
                        }
                    else
                        this.p.teleport(location);
                    if (this.w.size() > 1)
                        this.dist = this.w.get(0).distance((Location)this.w.get(1));
                    else
                        this.dist = 0;
                    ++this.i;
                    //this.ld = -1.0;
                    this.ls = -1.0;
                    this.npoint = true;
                    this.effpos = this.p.getLocation();
                }
                else {
                    try {
                        Location location2 = this.w.get(this.i);
                        Location prevLoc = this.w.get(this.i - 1);
                        Vector targetVec = cloneLocation(location2).subtract(cloneLocation(prevLoc)).toVector();
                        if (this.ws.get(this.i) >= 0.0) {
                            speed_a.put(this.u, this.ws.get(this.i));
                        }
                        // check radius, depends on speed
                        double n = speed_a.get(this.u) / 10.0 * M;
                        final Location location3 = effpos;
                        double ld;// = location3.distance(location2);
                        ld = smartDistance(location3, targetVec, location2);
                        //System.out.println("L=" + String.format("%7.3f", ld) + "  P=" + debugFmt(location3) + "  N=" + debugFmt(targetVec) + "  D=" + debugFmt(location2));
                        if (this.ls == -1.0) {
                            this.ls = n;
                        }
                        if (Math.abs(this.ls - n) > 0.5) {
                            n = this.ls + 0.5 * Math.signum(n - this.ls);
                        }
                        /*if (this.ld >= 0.0) {
                            if (this.dc && ld > this.ld) {
                                ld = 0.0;
                            }
                            if (ld < this.ld) {
                                this.dc = true;
                            }
                        }*/
                        if (ld <= n * (i >= w.size() - 1 ? ((wf & 0x10) != 0x0 ? 0.125 : 1) : 2) && totaldelay < 1) {
                            if ((this.wl.get(this.i)&1)>0) {
                                // insta-teleport to next point
                                this.i++;
                                while (this.i < this.wtemp.size() && wtemp.get(this.i)) {
                                    waypointEvent(this.windx.get(i));
                                    this.i++;
                                }
                                if (this.i >= this.wtemp.size()) {
                                    if (!pl_playing.get(u) && !fplay && owner != null) {
                                        owner.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cEnd of path reached.")));
                                    }
                                    waypoints_t.put(this.u, this.ticks_moved * (0.05D * M));
                                    if (this.w.size() > 0)
                                        this.p.teleport(this.w.get(this.w.size() - 1));
                                    if (!isInGlobalMode)
                                        stop(this.p, false, PathPlaybackStoppedEvent.StopCause.FINISHED);
                                    else
                                        stopGlobalUUID(this.u);
                                    this.cancel();
                                    return;
                                }
                                Location location5 = this.w.get(this.i);
                                location5.setPitch((float)(double)this.wp.get(this.i));
                                location5.setYaw((float)(double)this.wy.get(this.i));
                                if (!isInGlobalMode) {
                                    this.p.teleport(location5);
                                    // show waypoint message
                                    if (this.wmsg.get(this.i).length() > 0) {
                                        this.p.sendMessage((String)this.wmsg.get(this.i));
                                    }
                                    // run waypoint commands
                                    for (final String wc : this.wcmd.get(this.i)) {
                                        if (wc.startsWith("~")) {
                                            this.p.chat("/" + wc.substring(1));
                                        }
                                        else {
                                            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), wc.replace("%player%", this.p.getName()));
                                        }
                                    }
                                } else {
                                    for (Player ps: getServer().getOnlinePlayers()) {
                                        // show waypoint message
                                        if (this.wmsg.get(this.i).length() > 0) {
                                            ps.sendMessage((String)this.wmsg.get(this.i));
                                        }
                                        ps.teleport(location5);
                                        // run waypoint commands
                                        for (final String wc : this.wcmd.get(this.i)) {
                                            if (wc.startsWith("~")) {
                                                ps.chat("/" + wc.substring(1));
                                            }
                                            else {
                                                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), wc.replace("%player%", this.p.getName()));
                                            }
                                        }
                                    }
                                }
                                // total delay spent on waypoint
                                this.totaldelay += (int)(this.wd.get(this.i) * 20.0);
                                this.ticks_moved++;
                                return;
                            }
                        }
                        // check margin
                        double margin = n * (i >= w.size() - 1 ? ((wf & 0x10) != 0x0 ? 0.125 : 1) : 2);
                        while (ld <= margin && totaldelay < 1) {
                            ++this.i;
                            if (!this.isCurrent()) {
                                this.cancel();
                                return;
                            }
                            if (this.i >= this.w.size()) {
                                this.end = true;
                                return;
                            }
                            waypointEvent(this.windx.get(i));
                            prevLoc = location2;
                            location2 = this.w.get(this.i);
                            // direction to target
                            targetVec = cloneLocation(location2).subtract(cloneLocation(prevLoc)).toVector();
                            //ld = location3.distance(location2);
                            // new distance
                            ld = smartDistance(location3, targetVec, location2);
                            this.dist = this.w.get(this.i - 1).distance(location2);
                            //this.dc = false;
                            if (this.apoints.contains(this.i))
                                this.npoint = true;
                            this.oy = this.p.getLocation().getYaw();
                            this.op = this.p.getLocation().getPitch();
                            this.yaw = !improper(this.wy.get(this.i));
                            if (this.yaw) {
                                this.fy = this.optimal(this.oy, this.wy.get(this.i));
                                //System.out.println("Optimal yaw: " + this.fy);
                                //Bukkit.broadcastMessage("Y: " + this.oy + " -> " + this.fy);
                            }
                            this.pitch = !improper(this.wp.get(this.i));
                            if (this.pitch) {
                                this.fp = this.optimal(this.op, this.wp.get(this.i));
                            }
                            // show message
                            if (this.wmsg.get(this.i).length() > 0) {
                                if (!isInGlobalMode)
                                    this.p.sendMessage((String)this.wmsg.get(this.i));
                                else
                                    for (Player ps: getServer().getOnlinePlayers()) {
                                        ps.sendMessage((String)this.wmsg.get(this.i));
                                    }
                            }
                            // run commands
                            if (!isInGlobalMode)
                                for (final String wc : this.wcmd.get(this.i)) {
                                    if (wc.startsWith("~")) {
                                        this.p.chat("/" + wc.substring(1));
                                    }
                                    else {
                                        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), wc.replace("%player%", this.p.getName()));
                                    }
                                }
                            else
                                for (Player ps: getServer().getOnlinePlayers()) {
                                    for (final String wc : this.wcmd.get(this.i)) {
                                        if (wc.startsWith("~")) {
                                            ps.chat("/" + wc.substring(1));
                                        }
                                        else {
                                            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), wc.replace("%player%", ps.getName()));
                                        }
                                    }
                                }
                            // total delay on waypoint
                            this.totaldelay += (int)(this.wd.get(this.i) * 20.0);
                        }
                        double min = Math.min(ld, n);
                        if ((wf & 0x8) != 0x0) {
                            if (ticks_moved < 20) {
                                // smoothe starting velocity
                                min *= Math.min(1, ticks_moved / 20.0 * M);
                            }
                        }
                        if ((wf & 0x10) != 0x0) {
                            // try to estimate number of ticks remaining
                            int remainingWaypoints = this.w.size() - this.i;
                            if (remainingWaypoints < 15) {
                                double totalDist = 0;
                                for (int j = i; j < w.size() - 1; ++j) {
                                    totalDist += w.get(j).distance(w.get(j + 1)); 
                                }
                                totalDist += effpos.distance(w.get(i));
                                int ticksLeft = (int)Math.ceil(totalDist / n);
                                if (ticksLeft < 8) {
                                    // smoothe starting velocity
                                    min *= Math.min(1, Math.max(0.25, ticksLeft / 8.0 * M));
                                }
                            }
                        }
                        this.xm = min * ((location2.getX() - location3.getX()) / ld);
                        this.ym = min * ((location2.getY() - location3.getY()) / ld);
                        this.zm = min * ((location2.getZ() - location3.getZ()) / ld);
                        // magic number that was decided through trial-and-error
                        // trying to adjust for vertical momentum being less than horizontal momentum
                        this.ym *= 1.4678;
                        if (this.totaldelay > 0) {
                            --this.totaldelay;
                            this.ticks_moved++;
                            //this.ld = ld;
                            this.ls = n;
                            if (this.tm && (this.wf & 0x4) == 0) {
                                if (!isInGlobalMode) {
                                    this.p.teleport(this.p.getLocation());
                                } else {
                                    for (Player ps : getServer().getOnlinePlayers()) {
                                        ps.teleport(this.p.getLocation());
                                    }
                                }
                            }
                            return;
                        }
                        if (!this.isCurrent()) {
                            this.cancel();
                            return;
                        }
                        if (isInGlobalMode) {
                            for (final Player ps: TEMP_JOINS) {
                                ps.teleport(p);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (ps.getLocation().distanceSquared(p.getLocation()) < 0.01) {
                                            this.cancel();
                                            return;
                                        }
                                        try { 
                                            ps.teleport(p); 
                                        } catch (Exception ex) { 
                                            this.cancel();
                                            return;
                                        }
                                    }
                                }.runTaskTimer(ServerCinematics.this, 20L, 20L);
                            }
                            TEMP_JOINS.clear();
                        }
                        if (this.tm) { // tpmode
                            final Location location4 = this.p.getLocation();
                            this.q = (-(location4.distance((Location)this.w.get(this.i)) - margin) + this.dist) / this.dist;
                            //this.q = (this.dist - location4.distance(location2)) / this.dist;
                            this.q = Math.min(1, Math.max(0, this.q));
                            if (this.yaw) {
                                location4.setYaw((float)formatAngleYaw(this.oy + this.fy * this.q));
                            }
                            if (this.pitch) {
                                location4.setPitch((float)formatAnglePitch(this.op + this.fp * this.q));
                            }
                            location4.add(this.xm, this.ym, this.zm);
                            effpos = location4;
                            if (this.nopath) {
                                if (this.npoint) {
                                    int ind = Math.max(0, this.i);
                                    Location loc5 = cloneLocation(this.w.get(ind));
                                    if (this.yaw) loc5.setYaw((float)(double)this.wy.get(ind));
                                    else loc5.setYaw(effpos.getYaw());
                                    if (this.pitch) loc5.setPitch((float)(double)this.wp.get(ind));
                                    else loc5.setPitch(effpos.getPitch());
                                    if (!isInGlobalMode)
                                        this.p.teleport(loc5);
                                    else
                                        for (Player ps: getServer().getOnlinePlayers()) {
                                            ps.teleport(loc5);
                                        }
                                    this.npoint = false;
                                }
                            } else {
                                if (!isInGlobalMode)
                                    this.p.teleport(location4);
                                else
                                    for (Player ps: getServer().getOnlinePlayers()) {
                                        ps.teleport(location4);
                                    }
                            }
                        }
                        else { // use velocity instead of teleport
                            if (!isInGlobalMode)
                                this.p.setVelocity(new Vector(this.xm, this.ym, this.zm));
                            else
                                for (Player ps: getServer().getOnlinePlayers()) {
                                    ps.setVelocity(new Vector(this.xm, this.ym, this.zm));
                                }
                        }
                        this.ticks_moved++;
                        //this.ld = ld;
                        this.ls = n;
                    }
                    catch (Exception ex) {
                        if (!this.isCurrent()) {
                            this.cancel();
                            return;
                        }
                        if (owner != null) {
                            owner.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cAn error occurred during play. See the console.")));
                        }
                        this.cancel();
                        if (!isInGlobalMode) stop(this.p, PathPlaybackStoppedEvent.StopCause.FINISHED);
                        else stopGlobalUUID(this.u);
                        ex.printStackTrace();
                    }
                }
            }

            private double smartDistance(Location p, Vector v, Location dest) {
                if (v.lengthSquared() < 0.0001) {
                    return p.distance(dest);
                }
                // distance of p from a plane on which dest is and that v is a normal to
                // use Hessian plane formula to compute distance
                Vector vm = v.normalize();
                return vm.dot(dest.toVector()) - vm.dot(p.toVector());
            }
            
            // do waypoint reached event
            private void waypointEvent(int i) {
                if (i >= 0) {
                    WaypointReachedEvent evt = new WaypointReachedEvent(p, user.getPathName(), i, waypoints.get(p.getUniqueId()).size());
                    getServer().getPluginManager().callEvent(evt);
                }
            }

            // really unoptimized way to get the best direction to turn between two degree angles
            private double optimal(final double n, final double n2) {
                double o = n2 - n;
                while (o < -180) {
                    o += 360;
                }
                while (o > 180) {
                    o -= 360;
                }
                return o;
            }
            
/*
            private double optimal(final double n, final double n2) {
                return optimalRaw(n + 180, n2 + 180);
            }

            private double optimalRaw(final double n, final double n2) {
                final double n3 = 180.0 - Math.abs(Math.abs(n - n2 + 360.0) - 180.0);
                if (180.0 - Math.abs(Math.abs(n - n2 + 361.0) - 180.0) > n3) {
                    return -n3;
                }
                return n3;
            }*/
        }.runTaskTimer(this, 1L, 1L);
        return id;
    }
    
    protected Location cloneLocation(Location location) {
        return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    double formatAngleYaw(double n) {
        double v = (n + 180.0) % 360.0;
        if (v < 0.0) {
            v += 360.0;
        }
        return v - 180.0;
    }
    double formatAnglePitch(double n) {
        double v = (n + 90.0) % 180.0;
        if (v < 0.0) {
            v += 180.0;
        }
        return v - 90.0;
    }
    
    protected String fts(final double n) {
        return Double.toString(Math.floor(n * 1000.0) / 1000.0);
    }
    
    private boolean isFalse(final Boolean b) {
        return !this.isTrue(b);
    }
    
    private boolean isTrue(final Boolean b) {
        return b != null && b;
    }
    
    private List<Location> getSafeWaypoints(final Player player) {
        if (this.waypoints.get(player.getUniqueId()) == null) {
            this.waypoints.put(player.getUniqueId(), new ArrayList<Location>());
        }
        return this.waypoints.get(player.getUniqueId());
    }
    
    public List<Location> getSafeWaypoints(final CommandSender commandSender) {
        return this.getSafeWaypoints((Player)commandSender);
    }
    
    private List<Double> getSafeWaypointSpeeds(final Player player) {
        if (this.waypoints_s.get(player.getUniqueId()) == null) {
            this.waypoints_s.put(player.getUniqueId(), new ArrayList<Double>());
        }
        return this.waypoints_s.get(player.getUniqueId());
    }
    
    private List<Double> getSafeWaypointPitch(final Player player) {
        if (this.waypoints_p.get(player.getUniqueId()) == null) {
            this.waypoints_p.put(player.getUniqueId(), new ArrayList<Double>());
        }
        return this.waypoints_p.get(player.getUniqueId());
    }
    
    private List<Double> getSafeWaypointYaw(final Player player) {
        if (this.waypoints_y.get(player.getUniqueId()) == null) {
            this.waypoints_y.put(player.getUniqueId(), new ArrayList<Double>());
        }
        return this.waypoints_y.get(player.getUniqueId());
    }
    
    private List<String> getSafeWaypointMessages(final Player player) {
        if (this.waypoints_m.get(player.getUniqueId()) == null) {
            this.waypoints_m.put(player.getUniqueId(), new ArrayList<String>());
        }
        return this.waypoints_m.get(player.getUniqueId());
    }
    
    private List<List<String>> getSafeWaypointCommands(final Player player) {
        if (this.waypoints_c.get(player.getUniqueId()) == null) {
            this.waypoints_c.put(player.getUniqueId(), new ArrayList<List<String>>());
        }
        return this.waypoints_c.get(player.getUniqueId());
    }
    
    public List<Double> getSafeWaypointDelays(final Player player) {
        if (this.waypointDelays.get(player.getUniqueId()) == null) {
            this.waypointDelays.put(player.getUniqueId(), new ArrayList<Double>());
        }
        return this.waypointDelays.get(player.getUniqueId());
    }
    
    public List<Integer> getSafeWaypointOptions(final Player player) {
        if (this.waypointOptions.get(player.getUniqueId()) == null) {
            this.waypointOptions.put(player.getUniqueId(), new ArrayList<Integer>());
        }
        return this.waypointOptions.get(player.getUniqueId());
    }
    
    public List<Boolean> getSafeWaypointInstants(final Player player) {
        if (this.waypoints_i.get(player.getUniqueId()) == null) {
            this.waypoints_i.put(player.getUniqueId(), new ArrayList<Boolean>());
        }
        return this.waypoints_i.get(player.getUniqueId());
    }
    
    private int getSafeWaypointFlags(final Player player) {
        if (!this.waypoints_f.containsKey(player.getUniqueId())) {
            this.waypoints_f.put(player.getUniqueId(), 0);
        }
        return this.waypoints_f.get(player.getUniqueId());
    }

    
    private List<Location> getSafeWaypoints(final UUID u) {
        if (this.waypoints.get(u) == null) {
            this.waypoints.put(u, new ArrayList<Location>());
        }
        return this.waypoints.get(u);
    }
    
    private List<Double> getSafeWaypointSpeeds(final UUID u) {
        if (this.waypoints_s.get(u) == null) {
            this.waypoints_s.put(u, new ArrayList<Double>());
        }
        return this.waypoints_s.get(u);
    }
    
    private List<Double> getSafeWaypointPitch(final UUID u) {
        if (this.waypoints_p.get(u) == null) {
            this.waypoints_p.put(u, new ArrayList<Double>());
        }
        return this.waypoints_p.get(u);
    }
    
    private List<Double> getSafeWaypointYaw(final UUID u) {
        if (this.waypoints_y.get(u) == null) {
            this.waypoints_y.put(u, new ArrayList<Double>());
        }
        return this.waypoints_y.get(u);
    }
    
    private List<String> getSafeWaypointMessages(final UUID u) {
        if (this.waypoints_m.get(u) == null) {
            this.waypoints_m.put(u, new ArrayList<String>());
        }
        return this.waypoints_m.get(u);
    }
    
    private List<List<String>> getSafeWaypointCommands(final UUID u) {
        if (this.waypoints_c.get(u) == null) {
            this.waypoints_c.put(u, new ArrayList<List<String>>());
        }
        return this.waypoints_c.get(u);
    }
    
    /*private ArrayList<Double> getSafeWaypointDelays(final UUID u) {
        if (this.waypointDelays.get(u) == null) {
            this.waypointDelays.put(u, new ArrayList<Double>());
        }
        return this.waypointDelays.get(u);
    }
    
    private ArrayList<Integer> getSafeWaypointOptions(final UUID u) {
        if (this.waypointOptions.get(u) == null) {
            this.waypointOptions.put(u, new ArrayList<Integer>());
        }
        return this.waypointOptions.get(u);
    }*/
    
    private int getSafeWaypointFlags(final UUID u) {
        if (!this.waypoints_f.containsKey(u)) {
            this.waypoints_f.put(u, 0);
        }
        return this.waypoints_f.get(u);
    }
    
    private List<Double> getSafeWaypointSpeeds(final CommandSender commandSender) {
        return this.getSafeWaypointSpeeds((Player)commandSender);
    }
    
    public List<Double> getSafeWaypointPitch(final CommandSender commandSender) {
        return this.getSafeWaypointPitch((Player)commandSender);
    }
    
    public List<Double> getSafeWaypointYaw(final CommandSender commandSender) {
        return this.getSafeWaypointYaw((Player)commandSender);
    }
    
    private List<String> getSafeWaypointMessages(final CommandSender commandSender) {
        return this.getSafeWaypointMessages((Player)commandSender);
    }
    
    private List<List<String>> getSafeWaypointCommands(final CommandSender commandSender) {
        return this.getSafeWaypointCommands((Player)commandSender);
    }
    
    private List<Integer> getSafeWaypointOptions(final CommandSender commandSender) {
        return this.getSafeWaypointOptions((Player)commandSender);
    }

    private List<Boolean> getSafeWaypointInstants(CommandSender commandSender) {
        return this.getSafeWaypointInstants((Player)commandSender);
    }
    
    private List<Double> getSafeWaypointDelays(final CommandSender commandSender) {
        return this.getSafeWaypointDelays((Player)commandSender);
    }
    
    private void clearCache(final Player player) {
        this.wm.put(player.getUniqueId(), false);
        this.waypoints_t.put(player.getUniqueId(), -1.0);
    }
    
    private void clearPathName(final Player player) {
        this.pathnames.put(player.getUniqueId(), MODIFIED_PATH);
    }
    
    private boolean hasCache(final Player player) {
        return this.getCache(player) != null && this.getIsCacheUpToDate(player);
    }
    
    private boolean getIsCacheUpToDate(final Player player) {
        final Boolean b = this.wm.get(player.getUniqueId());
        return b != null && b;
    }
    
    private List<Location> getCache(final Player player) {
        return this.wx.get(player.getUniqueId());
    }
    
    protected String lts(final Location location) {
        return "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
    }
    
    protected String lts2(final Location location) {
        return "(" + this.fts(location.getX()) + "," + this.fts(location.getY()) + "," + this.fts(location.getZ()) + ")";
    }
    
    // three-dimensional Catmull-Rom spline point computation
    static Location catmull_rom_3d(final double n, final Location location, final Location location2, final Location location3, final Location location4) {
        if (n > 1.0 || n < 0.0) {
            throw new IllegalArgumentException("s must be between 0.0 and 1.0 (was: " + Double.toString(n) + ")");
        }
        double x1 = location.getX();
        double y1 = location.getY();
        double z1 = location.getZ();
        double x2 = location2.getX();
        double y2 = location2.getY();
        double z2 = location2.getZ();
        double x3 = location3.getX();
        double y3 = location3.getY();
        double z3 = location3.getZ();
        double x4 = location4.getX();
        double y4 = location4.getY();
        double z4 = location4.getZ();
        return new Location(location.getWorld(), 0.5 * (2.0 * x2 + (x3 - x1) * n + (2.0 * x1 - 5.0 * x2 + 4.0 * x3 - x4) * n * n + (x4 - 3.0 * x3 + 3.0 * x2 - x1) * n * n * n), 0.5 * (2.0 * y2 + (y3 - y1) * n + (2.0 * y1 - 5.0 * y2 + 4.0 * y3 - y4) * n * n + (y4 - 3.0 * y3 + 3.0 * y2 - y1) * n * n * n), 0.5 * (2.0 * z2 + (z3 - z1) * n + (2.0 * z1 - 5.0 * z2 + 4.0 * z3 - z4) * n * n + (z4 - 3.0 * z3 + 3.0 * z2 - z1) * n * n * n));
    }

    public void stop(final Player player, final PathPlaybackStoppedEvent.StopCause cause) {
        stop(player, true, cause);
    }
    private void stop(final Player player, boolean noPlaylist, final PathPlaybackStoppedEvent.StopCause cause) {
        User user = getUserManager().getUser(player.getUniqueId());
        final UUID uniqueId = player.getUniqueId();
        if (noPlaylist)
            pl_playing.put(uniqueId, false);
        boolean wasPlaying = isTrue(this.playing.get(uniqueId));
        this.paused.put(uniqueId, false);
        this.playing.put(uniqueId, false);
        try {
            player.setGameMode((GameMode)this.old_gm.get(uniqueId));
        }
        catch (Exception ex) {}
        try {
            player.setFlySpeed((float)this.old_fsp.get(uniqueId));
        }
        catch (Exception ex) {}
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        if ((this.getSafeWaypointFlags(player) & 0x1) != 0x0) {
            player.teleport((Location)this.getSafeWaypoints(player).get(0));
        }
        if ((this.getSafeWaypointFlags(player) & 0x2) != 0x0) {
            player.teleport((Location)this.old_loc.get(uniqueId));
        }
        else {
            try {
                player.setAllowFlight((boolean)this.old_af.get(uniqueId));
            }
            catch (Exception ex2) {}
            try {
                player.setFlying((boolean)this.old_f.get(uniqueId));
            }
            catch (Exception ex3) {}
        }
        if (wasPlaying) {
            PathPlaybackStoppedEvent evt = new PathPlaybackStoppedEvent(cause, player, user.getPathName(), pbids.get(player.getUniqueId()));
            getServer().getPluginManager().callEvent(evt);
        }
        if (pl_playing.get(uniqueId)) {
            if (!this.findNextSuitablePath(player)) {
                if (pl_looping.get(uniqueId)) {
                    player.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cNo more playable paths found.")));
                } else {
                    player.sendMessage(Utils.colorize((shortPrefix ? "&7[&8&l[**]<|&7]" : "&7[&7&l[**]<|&eServer&6Cinematics&7] &cEnd of playlist.")));
                }
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    play(player, player, false, StartCause.PLAYLIST);
                }
            }.runTaskLater(this, 1L);
        }
    }

    private void stopGlobalUUID(final UUID uniqueId) {
        this.paused.put(uniqueId, false);
        this.playing.put(uniqueId, false);
        if (pl_playing.get(uniqueId)) {
            if (!this.findNextSuitablePath(uniqueId)) {
                stopGlobal();
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player p = getServer().getOnlinePlayers().iterator().next();
                    playWithFakeUUID(p, p, uniqueId, false, StartCause.PLAYLIST);
                }
            }.runTaskLater(this, 1L);
        }
    }

    String lastWorld = "";
    private boolean findNextSuitablePath(Player player) {
        UUID u = player.getUniqueId();
        int old_index = pl_index.get(u);
        int check_index = old_index;
        List<String> list = pl_paths.get(u);
        int length = list.size();
        boolean double_loop = false;
        while (true) {
            if (++check_index == old_index) {
                // no paths;
                return false;
            }
            if (check_index == length) {
                if (pl_looping.get(u)) {
                    if (double_loop) return false;
                    double_loop = true;
                    check_index = -1;
                    continue;
                } else
                    return false;
            } 
            if (this.loadForPlaylist(player, list.get(check_index), true) > 0) {
                pl_index.put(u, check_index);
                return true;
            }
        }
    }
    private boolean findNextSuitablePath(UUID u) {
        int old_index = pl_index.get(u);
        int check_index = old_index;
        List<String> list = pl_paths.get(u);
        int length = list.size();
        boolean double_loop = false;
        while (true) {
            if (++check_index == old_index) {
                // no paths;
                return false;
            }
            if (check_index == length) {
                if (pl_looping.get(u)) {
                    if (double_loop) return false;
                    double_loop = true;
                    check_index = -1;
                    continue;
                } else
                    return false;
            } 
            if (this.loadForPlaylist(u, list.get(check_index)) > 0) {
                pl_index.put(u, check_index);
                return true;
            }
        }
    }

    private int loadForPlaylist(Player player, String string, boolean teleport) {
        int res = loadForPlaylist(player.getUniqueId(), string);
        if (res > 0 && teleport) {
            try {
                player.teleport(this.getSafeWaypoints(player).get(0));
            } catch (Exception ex) {
                return -1;
            }
        }
        return res;
    }

    private int loadForPlaylist(UUID u, String string) {
        try {
            final File file3 = new File(this.paths, string);
            if (!file3.isFile()) {
                return -2;
            }
            final String s8 = new String(Files.readAllBytes(file3.toPath()), StandardCharsets.UTF_8);
            int n11 = 0;
            this.clearCache(u);
            this.waypoints.put(u, new ArrayList<Location>());
            this.waypoints_s.put(u, new ArrayList<Double>());
            this.waypoints_y.put(u, new ArrayList<Double>());
            this.waypoints_p.put(u, new ArrayList<Double>());
            this.waypoints_m.put(u, new ArrayList<String>());
            this.waypoints_c.put(u, new ArrayList<List<String>>());
            this.waypoints_f.put(u, 0);
            this.waypoints_t.put(u, -1.0);
            this.waypointDelays.put(u, new ArrayList<Double>());
            this.waypointOptions.put(u, new ArrayList<Integer>());
            this.waypoints_i.put(u, new ArrayList<Boolean>());
            final List<Location> safeWaypoints7 = this.getSafeWaypoints(u);
            final List<Double> safeWaypointSpeeds7 = this.getSafeWaypointSpeeds(u);
            final List<Double> safeWaypointYaw9 = this.getSafeWaypointYaw(u);
            final List<Double> safeWaypointPitch9 = this.getSafeWaypointPitch(u);
            final List<String> safeWaypointMessages3 = this.getSafeWaypointMessages(u);
            final List<List<String>> safeWaypointCommands3 = this.getSafeWaypointCommands(u);
            //final World world3 = player4.getWorld();
            final String s9 = s8.split("#")[0];
            final String s10 = s8.split("#")[1];
            final float float3 = Float.parseFloat(s9.split(",")[2]);
            final float float4 = Float.parseFloat(s9.split(",")[3]);
            int n12 = 0;
            int safeFlags2 = 0;
            this.pathnames.put(u, string);
            if (s9.split(",").length > 4) {
                safeFlags2 = Integer.parseInt(s9.split(",")[4]);
            }
            if (Bukkit.getServer().getWorld(s9.split(",")[0]) == null) {
                return -1;
            }
            double safeTime = -1;
            if (s9.split(",").length > 5) {
                safeTime = Double.parseDouble(s9.split(",")[5]);
            }
            final World aworld = Bukkit.getServer().getWorld(s9.split(",")[0]);
            String[] split6;
            for (int length4 = (split6 = s10.split(Pattern.quote("|"))).length, n13 = 0; n13 < length4; ++n13) {
                final String s11 = split6[n13];
                try {
                    final String[] split7 = s11.split(",");
                    final Location location6 = new Location(aworld, Double.parseDouble(split7[0]), Double.parseDouble(split7[1]), Double.parseDouble(split7[2].split(";")[0]));
                    if (n12 == 0) {
                        location6.setYaw(float3);
                        location6.setPitch(float4);
                    }
                    safeWaypoints7.add(location6);
                    safeWaypointCommands3.add(new ArrayList<String>());
                    if (split7[3].indexOf(10) >= 0) {
                        safeWaypointSpeeds7.add(Double.parseDouble(split7[3].split("\n")[0]));
                        int n14 = 0;
                        String[] split8;
                        for (int length5 = (split8 = split7[3].split("\n")).length, n15 = 0; n15 < length5; ++n15) {
                            final String s12 = split8[n15];
                            if (n14++ >= 1) {
                                safeWaypointCommands3.get(n12).add(s12.replace("\uf555", ","));
                            }
                        }
                    }
                    else {
                        safeWaypointSpeeds7.add(Double.parseDouble(split7[3]));
                    }
                    try {
                        if (split7[2].split(";").length > 3) {
                            this.waypoints_i.get(u).add(!split7[2].split(";")[3].equalsIgnoreCase("0"));
                        } else {
                            this.waypoints_i.get(u).add(false);
                        }
                        if (split7[2].split(";").length < 2) {
                            throw new ArrayIndexOutOfBoundsException();
                        }
                        double d = Double.parseDouble(split7[2].split(";")[1]);
                        int lf = Integer.parseInt(split7[2].split(";")[2]);
                        this.waypointDelays.get(u).add(d);
                        this.waypointOptions.get(u).add(lf);
                    } catch (Exception ex) {
                        this.waypointDelays.get(u).add(0.0);
                        this.waypointOptions.get(u).add(0);
                    }
                    if (split7.length > 4) {
                        final String[] split9 = split7[4].split(":");
                        final String[] split10 = split9[1].split("\\$", 2);
                        if (split10.length > 1) {
                            safeWaypointMessages3.add(split10[1].replace("\uf555", ","));
                        }
                        else {
                            safeWaypointMessages3.add("");
                        }
                        safeWaypointYaw9.add(this.formatAngleYaw(Double.parseDouble(split9[0])));
                        safeWaypointPitch9.add(this.formatAnglePitch(Double.parseDouble(split10[0])));
                    }
                    else {
                        safeWaypointYaw9.add(444.0);
                        safeWaypointPitch9.add(444.0);
                    }
                    ++n12;
                }
                catch (Exception ex15) {
                    if (safeWaypointYaw9.size() > safeWaypointPitch9.size()) {
                        safeWaypointYaw9.remove(safeWaypointYaw9.size() - 1);
                    }
                    if (safeWaypointMessages3.size() > safeWaypointYaw9.size()) {
                        safeWaypointMessages3.remove(safeWaypointMessages3.size() - 1);
                    }
                    if (safeWaypointSpeeds7.size() > safeWaypointYaw9.size()) {
                        safeWaypointSpeeds7.remove(safeWaypointSpeeds7.size() - 1);
                    }
                    if (safeWaypoints7.size() > safeWaypointSpeeds7.size()) {
                        safeWaypoints7.remove(safeWaypoints7.size() - 1);
                    }
                    if (safeWaypointCommands3.size() > safeWaypoints7.size()) {
                        safeWaypointCommands3.remove(safeWaypointCommands3.size() - 1);
                    }
                    ++n11;
                }
            }
            this.waypoints.put(u, safeWaypoints7);
            this.waypoints_s.put(u, safeWaypointSpeeds7);
            this.waypoints_y.put(u, safeWaypointYaw9);
            this.waypoints_p.put(u, safeWaypointPitch9);
            this.waypoints_m.put(u, safeWaypointMessages3);
            this.waypoints_c.put(u, safeWaypointCommands3);
            this.waypoints_f.put(u, safeFlags2);
            this.waypoints_t.put(u, safeTime);
            this.speed.put(u, Double.parseDouble(s9.split(",")[1]));
            if (safeWaypoints7.size() == 0)
                return 0;
            lastWorld = s9.split(",")[0];
            return n12 - n11;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    
    private void deserializePath(Player player, CinematicPath path) {
        final UUID u = player.getUniqueId();
        this.waypoints.put(u, new ArrayList<Location>());
        this.waypoints_s.put(u, new ArrayList<Double>());
        this.waypoints_y.put(u, new ArrayList<Double>());
        this.waypoints_p.put(u, new ArrayList<Double>());
        this.waypoints_m.put(u, new ArrayList<String>());
        this.waypoints_c.put(u, new ArrayList<List<String>>());
        this.waypointDelays.put(u, new ArrayList<Double>());
        this.waypointOptions.put(u, new ArrayList<Integer>());
        this.waypoints_i.put(u, new ArrayList<Boolean>());
        final List<Location> safeWaypoints7 = this.getSafeWaypoints(u);
        final List<Double> safeWaypointSpeeds7 = this.getSafeWaypointSpeeds(u);
        final List<Double> safeWaypointYaw9 = this.getSafeWaypointYaw(u);
        final List<Double> safeWaypointPitch9 = this.getSafeWaypointPitch(u);
        final List<String> safeWaypointMessages3 = this.getSafeWaypointMessages(u);
        final List<List<String>> safeWaypointCommands3 = this.getSafeWaypointCommands(u);
        final List<Double> safeWaypointDelays = this.getSafeWaypointDelays(player);
        final List<Integer> safeWaypointOptions = this.getSafeWaypointOptions(player);
        final List<Boolean> safeWaypointInstants = this.getSafeWaypointInstants(player);

        this.waypoints_f.put(u, (path.shouldTeleportToStartAfterPlayback() ? 1 : 0)
                              | (path.shouldTeleportBackAfterPlayback() ? 2 : 0)
                              | (path.canPlayerTurnCameraDuringDelay() ? 4 : 0));
        this.waypoints_t.put(u, -1.0);
        
        for (CinematicWaypoint wp: path.getWaypoints()) {
            double speed = wp.getSpeed();
            double yaw = wrapAngle(wp.getYaw());
            double pitch = wrapAngle(wp.getPitch());

            if (improper(yaw)) {
                yaw = 444;
            }
            if (improper(pitch)) {
                pitch = 444;
            }
            
            safeWaypoints7.add(wp.getLocation());
            safeWaypointSpeeds7.add(speed);
            safeWaypointYaw9.add(yaw);
            safeWaypointPitch9.add(pitch);
            safeWaypointMessages3.add(Objects.toString(wp.getMessage(), ""));
            safeWaypointCommands3.add(wp.getCommands());
            safeWaypointDelays.add(wp.getDelay());
            safeWaypointOptions.add((wp.isInstant() ? 1 : 0));
            safeWaypointInstants.add(false);
        }

        this.pathnames.put(u, "");
    }

    private double wrapAngle(double angle) {
        if (Double.isFinite(angle)) {
            return ((((angle + 180.0) % 360.0) + 360.0) % 360.0) - 180.0;
        } else {
            return angle;
        }
    }

    private void clearCache(final UUID u) {
        this.wm.put(u, false);
        this.waypoints_t.put(u, -1.0);
    }

    boolean improper(final double n) {
        return Math.abs(n) > 360.0 || !Double.isFinite(n);
    }
    
    double properYaw(double n) {
        if (n < -180)
            n += 360;
        if (n > 180)
            n -= 360;
        return n;
    }

    public boolean shouldUseShortPrefix() {
        return shortPrefix;
    }

    public UserManager getUserManager() {
        return USER_MANAGER;
    }

    public boolean isInGlobalMode() {
        return isInGlobalMode;
    }

    public void setIsInGlobalMode(boolean isInGlobalMode) {
        this.isInGlobalMode = isInGlobalMode;
    }

}
