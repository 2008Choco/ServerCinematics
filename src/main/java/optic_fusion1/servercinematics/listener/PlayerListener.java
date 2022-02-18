package optic_fusion1.servercinematics.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import optic_fusion1.servercinematics.ServerCinematicsPlugin;
import optic_fusion1.servercinematics.user.User;
import optic_fusion1.servercinematics.user.UserManager;

public class PlayerListener implements Listener {

    private UserManager userManager;
    private ServerCinematicsPlugin plugin;

    public PlayerListener(ServerCinematicsPlugin plugin) {
        userManager = plugin.getUserManager();
        this.plugin = plugin;
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        userManager.addUser(new User(player.getUniqueId()));
        if (!plugin.isInGlobalMode()) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        // TEMP_JOINS.add(player);
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        User user = userManager.getUser(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        userManager.removeUser(player.getUniqueId());
        if (plugin.isInGlobalMode()) {
            Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(spawnLocation);
            GameMode defaultGamemode = Bukkit.getDefaultGameMode();
            player.setGameMode(defaultGamemode);
            player.setAllowFlight(defaultGamemode == GameMode.CREATIVE || defaultGamemode == GameMode.SPECTATOR);
            player.setFlying(defaultGamemode == GameMode.SPECTATOR);
            return;
        }
        if (user.isPlaying()) {
            // TODO: Reimplement
            // plugin.stop(player, PathPlaybackStoppedEvent.StopCause.LEFT);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void on(PlayerToggleFlightEvent event) {
        User user = userManager.getUser(event.getPlayer().getUniqueId());
        Player player = user.getPlayer();
        if (user.isPlaying()) {
            player.setAllowFlight(true);
            player.setFlying(true);
            event.setCancelled(true);
            return;
        }
        if (!plugin.isInGlobalMode()) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void on(PlayerTeleportEvent event) {
        User user = userManager.getUser(event.getPlayer().getUniqueId());
        if (user.isPlaying() && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void on(PlayerCommandPreprocessEvent event) {
        User user = userManager.getUser(event.getPlayer().getUniqueId());
        if (!user.isPlaying()) {
            return;
        }
        String message = event.getMessage().toLowerCase();
        String[] strings = message.split(" ")[0].split(":");
        for (String command : plugin.getConfig().getStringList("blacklist-command-list")) {
            if (command.startsWith(":")) {
                String substring = command.substring(1);
                if (message.startsWith("/" + substring)) {
                    event.setCancelled(true);
                }
                if (strings.length > 1 && strings[1].equalsIgnoreCase(substring)) {
                    event.setCancelled(true);
                }
            }
            if (message.startsWith("/" + command)) {
                event.setCancelled(true);
            }
        }
    }

}
