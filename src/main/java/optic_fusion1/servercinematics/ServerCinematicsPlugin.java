package optic_fusion1.servercinematics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import optic_fusion1.servercinematics.cinematic.CinematicManager;
import optic_fusion1.servercinematics.cinematic.CinematicPath;
import optic_fusion1.servercinematics.cinematic.SmoothKeyframe;
import optic_fusion1.servercinematics.cinematic.StaticKeyframe;
import optic_fusion1.servercinematics.command.CameraCommand;
import optic_fusion1.servercinematics.listener.PlayerListener;
import optic_fusion1.servercinematics.user.UserManager;

public final class ServerCinematicsPlugin extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CinematicPath.class, CinematicPath.SERIALIZER)
            .registerTypeAdapter(SmoothKeyframe.class, SmoothKeyframe.SERIALIZER)
            .registerTypeAdapter(StaticKeyframe.class, StaticKeyframe.SERIALIZER)
            .create();

    private File pathsDirectory = new File(getDataFolder(), "paths");

    private boolean shortPrefix = false;
    private boolean finalWaypointTeleport = true;
    private boolean inGlobalMode = false;

    private final CinematicManager cinematicManager = new CinematicManager();
    private final UserManager userManager = new UserManager();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.pathsDirectory.mkdirs();

        this.registerCommandSafely("camera", new CameraCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        this.shortPrefix = getConfig().getBoolean("short-prefix", false);
        this.finalWaypointTeleport = getConfig().getBoolean("final-waypoint-teleport", true);
    }

    @Override
    public void onDisable() {
        this.cinematicManager.clearCinematics(); // TODO: Write cinematics to file
    }

    public CinematicManager getCinematicManager() {
        return cinematicManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public boolean isShortPrefix() {
        return shortPrefix;
    }

    public boolean isFinalWaypointTeleport() {
        return finalWaypointTeleport;
    }

    public void setInGlobalMode(boolean inGlobalMode) {
        this.inGlobalMode = inGlobalMode;
    }

    public boolean isInGlobalMode() {
        return inGlobalMode;
    }

    private void registerCommandSafely(String commandName, CommandExecutor executor) {
        PluginCommand pluginCommand = getCommand(commandName);
        if (pluginCommand == null) {
            return;
        }

        pluginCommand.setExecutor(executor);

        if (executor instanceof TabCompleter tabCompleter) {
            pluginCommand.setTabCompleter(tabCompleter);
        }
    }

}
