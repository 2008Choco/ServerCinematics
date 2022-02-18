package optic_fusion1.servercinematics.command;

import email.com.gmail.cosmoconsole.bukkit.camera.ServerCinematics;
import optic_fusion1.servercinematics.event.PathPlaybackStartedEvent;
import optic_fusion1.servercinematics.event.PathPlaybackStoppedEvent;
import static optic_fusion1.servercinematics.util.Constant.LONG_PREFIX;
import static optic_fusion1.servercinematics.util.Constant.SHORT_PREFIX;
import optic_fusion1.servercinematics.util.Utils;
import static optic_fusion1.servercinematics.util.Utils.clipList;
import static optic_fusion1.servercinematics.util.Utils.sendMultilineMessage;
import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class CameraCommand implements CommandExecutor, TabCompleter {
    private static final String HELP_STRING = """
            &c&l[**]<|&0Server&6Cimematics &fby Optic_Fusion1, CosmoConsole, Misko, Pietu1998
            /### help - Display this message
            /### add (speed) (yaw/N/C) (pitch/N/C) - Adds your location as a new waypoint to your path.
            /### insert id (speed) (yaw/N/C) (pitch/N/C) - Add waypoint to your present location, to your path at a specific position (and possibly make it change speed)
            /### edit id <speed / d(on't change)> [yaw/N/C/d] [pitch/N/C/d] - Edit properties of a waypoint
            /### clone player - Clone someone elses path
            /### msg id {set msg | setcolored msg | remove} - Set/remove a message to a waypoint
            /### cmd id {add | list | get | remove} - See/add/remove commands of a waypoint
            /### option (id) (option) - See all possible options for a waypoint or toggle one of them
            /### delay id delay - See, add or remove a waypoint delay
            /### flags - See all possible flags.
            /### flag id - Toggles a flag for the path.
            /### list - List waypoints in your path
            /### playlist {add | list | insert | remove | clear | play | loop} - See/add/remove commands of a waypoint
            /### goto id - Teleports to a waypoint
            /### remove (id) - Remove a waypoint from your path (default: last one)
            /### clear - Clear your path\\n/### load (file) - List saved paths or load one
            /### save file - Save the current path to a file\\n/### speed (speed) - Get / set flying speed
            /### play - Play your path
            /### tplay ((hours:)minutes:)seconds - Play your path with specific duration
            /### tpmode - Toggle tpmode (tpmode has pitch & yaw support but is less smooth)
            /### pathless - Toggle pathless mode (automatic tpmode + teleports to waypoints only)
            /### fplay player path (tp | notp | pathless) - Force the player to load and play a path (possibly in tpmode)
            /### ftplay player ((hh:)mm:)ss path (tp | notp | pathless) - Force the player to load and play a path with specific timespan (possibly in tpmode)
            /### fstop player - Force the player to stop the current path
            /### fclear player - Clear the path of another player
            /### pause - Pause the current path
            /### resume - Resume from last pause
            /### stop - Stop playing
            /### reload - Reload configuration";
            """;
    private ServerCinematics plugin;
    private boolean shortPrefix;

    public CameraCommand(ServerCinematics plugin) {
        this.plugin = plugin;
        this.shortPrefix = plugin.shouldUseShortPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Utils.colorize(LONG_PREFIX + "&fby Optic_Fusion1, CosmoConsole, Kisko, Pietu1998, &aVersion: " + plugin.getDescription().getVersion()));
            sender.sendMessage(Utils.colorize("&7See \"/cam help\" for help."));
            return true;
        }
        // TODO: Re-implement console support
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize((shortPrefix ? SHORT_PREFIX : LONG_PREFIX) + "Only players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        plugin.getSafeWaypointDelays(player);
        plugin.getSafeWaypointOptions(player);
        plugin.getSafeWaypointInstants(player);
        String arg = args[0];
        if (arg.equalsIgnoreCase("help")) {
            sendMultilineMessage(sender, HELP_STRING.replace("###", label), Utils.colorize("7"));
            return true;
        }
        if (arg.equalsIgnoreCase("list")) {
            handleList(sender);
            return true;
        }
        if (arg.equalsIgnoreCase("delay")) {
            handleDelay(sender, args);
            return true;
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        if (!hasPermission(sender, "servercinematics.edit")) {
            return;
        }
        sender.sendMessage(Utils.colorize((shortPrefix ? SHORT_PREFIX : LONG_PREFIX) + "&aList of waypoints in your path:"));
        int n = 0;
        List<Double> safeWaypointYaw = plugin.getSafeWaypointYaw(sender);
        List<Double> safeWaypointPitch = plugin.getSafeWaypointPitch(sender);
        for (Location location : plugin.getSafeWaypoints(sender)) {
            double yaw = safeWaypointYaw.get(n);
            double pitch = safeWaypointPitch.get(n);
            sender.sendMessage(Utils.colorize("&e" + n + "&7: &f" + location.getBlockX() + "," + location.getBlockZ() + ":" + ((yaw > 400.0) ? "-" : String.format(Locale.ENGLISH, "%.1f", yaw)) + "," + ((pitch > 400.0) ? "-" : String.format(Locale.ENGLISH, "%.1f", pitch))));
        }
        if (n == 0) {
            sender.sendMessage(Utils.colorize((shortPrefix ? SHORT_PREFIX : LONG_PREFIX) + "&cNone at all!"));
        }
    }

    private void handleDelay(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "servercinematics.delay")) {
            return;
        }
        if (plugin.isInGlobalMode()) {
            sender.sendMessage(Utils.colorize((shortPrefix ? SHORT_PREFIX : LONG_PREFIX) + "&cYou can't use this."));
            return;
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Utils.colorize((shortPrefix ? SHORT_PREFIX : LONG_PREFIX) + "&cYou can't use this."));
            return false;
        }
        return true;
    }

    /*

            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " delay id delaylength");
                return true;
            }
            if (array.length < 3) {
                int b = 0;
                final Player p = (Player)commandSender;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Current delay: " + String.format("%.2f", this.waypoints_d.get(p.getUniqueId()).get(b)));
                return true;
            }
            try {
                final double int7 = Double.parseDouble(array[2]);
                final Player p = (Player)commandSender;
                int b = 0;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                if (int7 < 0) {
                    throw new IndexOutOfBoundsException();
                }
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
                this.waypoints_d.get(p.getUniqueId()).set(b, int7);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Delay set.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid delay!");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
            }
        }
        else if (s2.equalsIgnoreCase("instant")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " instant id");
                return true;
            }
            try {
                final Player p = (Player)commandSender;
                int b = 0;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                this.clearCache((Player)commandSender);
                boolean newFlag = !this.waypoints_i.get(p.getUniqueId()).get(b);
                this.waypoints_i.get(p.getUniqueId()).set(b, newFlag);
                if (newFlag)
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Point is now instant. (Only works in tpmode)");
                else
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Point is no longer instant.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid point!");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
            }
        }
        else if (s2.equalsIgnoreCase("option")) {
        if (!commandSender.hasPermission("servercinematics.edit")) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (globalMode != null) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (array.length < 2) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " option id option_id");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "0 " + make_color('6') + "- " + make_color('f') + "Teleport player immediately to the next point from this waypoint after the delay (if tpmode enabled)");
        }
        if (array.length < 3) {
            if (commandSender instanceof Player) {
                int b = 0;
                final Player p = (Player)commandSender;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                int o = this.waypoints_l.get(p.getUniqueId()).get(b);
                StringBuilder sb = new StringBuilder();
                int n = 0;
                for (int a = 1; a >= 0; a <<= 1) {
                    if ((o&a)>0) {
                        sb.append(n);
                        sb.append(" ");
                    }
                    n++;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Currently enabled options: " + sb.toString());
            }
            return true;
        }
        try {
            final int int7 = Integer.parseInt(array[2]);
            final Player p = (Player)commandSender;
            int b = 0;
            try {
                b = Integer.parseInt(array[1]);
                if (b < 0) throw new ArrayIndexOutOfBoundsException();
                if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
            } catch (Exception ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                return true;
            }
            if (int7 < 0 || int7 > 0) {
                throw new IndexOutOfBoundsException();
            }
            final int o = this.waypoints_l.get(p.getUniqueId()).get(b) & 1 << int7;
            final boolean oldState = o != 0;
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            this.waypoints_l.get(p.getUniqueId()).set(b, o ^ 1 << int7);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Option " + int7 + " is now " + (oldState ? "" + make_color('c') + "OFF" : "" + make_color('a') + "ON") + "" + make_color('e') + ".");
        }
        catch (NumberFormatException ex3) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
        }
        catch (IndexOutOfBoundsException ex4) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Option not found.");
        }
        catch (Exception ex5) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
        }
    }
        else if (s2.equalsIgnoreCase("flags")) {
        if (!commandSender.hasPermission("servercinematics.edit")) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (globalMode != null) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "0 " + make_color('6') + "- " + make_color('f') + "Teleport player to first waypoint after path finishes");
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "1 " + make_color('6') + "- " + make_color('f') + "Teleport player to original location after path finishes");
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "2 " + make_color('6') + "- " + make_color('f') + "Allow player to turn during delay (automatically if not tpmode)");
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "3 " + make_color('6') + "- " + make_color('f') + "Smooth velocity from standstill when starting path");
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "4 " + make_color('6') + "- " + make_color('f') + "Smooth velocity from standstill when ending path");
    }
        else if (s2.equalsIgnoreCase("flag")) {
        if (!commandSender.hasPermission("servercinematics.edit")) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (globalMode != null) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (array.length < 2) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " flag flag_id");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "See all possible flags with " + make_color('c') + "/" + s + " flags");
            if (commandSender instanceof Player) {
                final Player p = (Player)commandSender;
                int o = this.getSafeWaypointFlags(p);
                StringBuilder sb = new StringBuilder();
                int n = 0;
                for (int a = 1; a >= 0; a <<= 1) {
                    if ((o&a)>0) {
                        sb.append(n);
                        sb.append(" ");
                    }
                    n++;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Currently enabled flags: " + sb.toString());
            }
            return true;
        }
        try {
            final int int7 = Integer.parseInt(array[1]);
            final Player p = (Player)commandSender;
            if (int7 < 0 || int7 > 4) {
                throw new IndexOutOfBoundsException();
            }
            final int o = this.getSafeWaypointFlags(p) & (1 << int7);
            final boolean oldState = o != 0;
            this.waypoints_t.put(p.getUniqueId(), -1.0);
            this.waypoints_f.put(p.getUniqueId(), this.getSafeWaypointFlags(p) ^ (1 << int7));
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Flag " + int7 + " is now " + (oldState ? "" + make_color('c') + "OFF" : "" + make_color('a') + "ON") + "" + make_color('e') + ".");
        }
        catch (NumberFormatException ex3) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
        }
        catch (IndexOutOfBoundsException ex4) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Flag not found.");
        }
        catch (Exception ex5) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle flag!");
        }
    }
        else if (s2.equalsIgnoreCase("reload")) {
        if (!commandSender.hasPermission("servercinematics.edit")) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (globalMode != null) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        this.reloadConfig();
        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Reloaded.");
    }
        else if (s2.equalsIgnoreCase("add")) {
        if (!commandSender.hasPermission("servercinematics.edit")) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        if (globalMode != null) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
            return true;
        }
        final Location location2 = ((Player)commandSender).getLocation();
        final List<Location> safeWaypoints = this.getSafeWaypoints(commandSender);
        if (safeWaypoints.size() > 0 && !safeWaypoints.get(0).getWorld().getName().equalsIgnoreCase(location2.getWorld().getName())) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot add waypoints to another world!");
            return true;
        }
        this.clearCache((Player)commandSender);
        this.clearPathName((Player)commandSender);
        safeWaypoints.add(location2);
        this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints);
        final List<Double> safeWaypointSpeeds = this.getSafeWaypointSpeeds(commandSender);
        final List<Double> safeWaypointYaw2 = this.getSafeWaypointYaw(commandSender);
        final List<Double> safeWaypointPitch2 = this.getSafeWaypointPitch(commandSender);
        double n2 = -1.0;
        if (array.length > 1) {
            try {
                n2 = Double.parseDouble(array[1]);
                n2 = Math.abs(n2);
            }
            catch (Exception ex16) {}
        }
        double double1 = 444.0;
        if (array.length > 2) {
            if (array[2].equalsIgnoreCase("c")) {
                double1 = ((Player)commandSender).getLocation().getYaw();
            }
            else if (array[2].equalsIgnoreCase("n")) {
                double1 = 444.0;
            }
            else {
                try {
                    double1 = Double.parseDouble(array[2]);
                }
                catch (Exception ex17) {}
            }
        }
        else {
            double1 = ((Player)commandSender).getLocation().getYaw();
        }
        double double2 = 444.0;
        if (array.length > 3) {
            if (array[3].equalsIgnoreCase("c")) {
                double2 = ((Player)commandSender).getLocation().getPitch();
            }
            else if (array[3].equalsIgnoreCase("n")) {
                double2 = 444.0;
            }
            else {
                try {
                    double2 = Double.parseDouble(array[3]);
                }
                catch (Exception ex18) {}
            }
        }
        else {
            double2 = ((Player)commandSender).getLocation().getPitch();
        }
        double1 = formatAngleYaw(double1);
        double2 = formatAnglePitch(double2);
        safeWaypointSpeeds.add(n2);
        safeWaypointYaw2.add(double1);
        safeWaypointPitch2.add(double2);
        UUID u = ((Player)commandSender).getUniqueId();
        this.waypoints_s.put(u, safeWaypointSpeeds);
        this.waypoints_y.put(u, safeWaypointYaw2);
        this.waypoints_p.put(u, safeWaypointPitch2);
        if (this.waypoints_m.get(u) == null) {
            this.waypoints_m.put(u, new ArrayList<String>());
        }
        this.waypoints_m.get(u).add("");
        if (this.waypoints_c.get(u) == null) {
            this.waypoints_c.put(u, new ArrayList<List<String>>());
        }
        this.waypoints_c.get(u).add(new ArrayList<String>());
        this.getSafeWaypointDelays(commandSender);
        this.getSafeWaypointOptions(commandSender);
        this.getSafeWaypointInstants(commandSender);
        this.waypoints_l.get(u).add(0);
        this.waypoints_d.get(u).add(0.0);
        this.waypoints_i.get(u).add(false);
        if (n2 >= 0.0) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added waypoint #" + (safeWaypoints.size() - 1) + ", setting the speed to " + n2);
        }
        else {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added waypoint #" + (safeWaypoints.size() - 1));
        }
    }
        else {
        return onCommand_2(commandSender, command, s, array);
    }
        return true;
}

    private boolean onCommand_2(final CommandSender commandSender, final Command command, final String s, final String[] array)
    {
        final String s2 = array[0];
        if (s2.equalsIgnoreCase("tpmode")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId = ((Player)commandSender).getUniqueId();
            if (this.pathless.containsKey(uniqueId) && this.pathless.get(uniqueId)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please disable pathless mode first.");
                return true;
            }
            final Boolean b = this.teleport.get(uniqueId);
            this.clearCache(uniqueId);
            if (this.teleport.containsKey(uniqueId) && b) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Teleport mode switched off for next path (yaw/pitch won't work, more smooth)");
                this.teleport.put(uniqueId, false);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Teleport mode switched on for next path (yaw/pitch will work, less smooth)");
                this.teleport.put(uniqueId, true);
            }
            return true;
        } else if (s2.equalsIgnoreCase("pathless")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId = ((Player)commandSender).getUniqueId();
            final Boolean b = this.pathless.get(uniqueId);
            this.clearCache(((Player)commandSender));
            if (this.pathless.containsKey(uniqueId) && b) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Pathless switched off (tpmode configurable)");
                this.pathless.put(uniqueId, false);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Pathless switched on (automatic tpmode)");
                this.pathless.put(uniqueId, true);
                this.teleport.put(uniqueId, true);
            }
            return true;
        } else if (s2.equalsIgnoreCase("cmd")) {
            if (!commandSender.hasPermission("servercinematics.edit") || !commandSender.hasPermission("servercinematics.cmd")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " cmd id { list | add commandwithout/ | get index | remove index }");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "%player% will be replaced with the player's name");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "command starting with a ~ will be run exactly as if the player");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "    him- or herself had typed it, without the starting ~ of course.");
                return true;
            }
            int int8;
            try {
                int8 = Integer.parseInt(array[1]);
            }
            catch (Exception ex6) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                return true;
            }
            final List<List<String>> safeWaypointCommands = this.getSafeWaypointCommands(commandSender);
            if (int8 < 0 || int8 >= safeWaypointCommands.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            if (array.length <= 2) {
                return true;
            }
            if (array[2].equalsIgnoreCase("list")) {
                int n3 = 0;
                final Iterator<String> iterator2 = safeWaypointCommands.get(int8).iterator();
                while (iterator2.hasNext()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "" + n3++ + " " + make_color('f') + "-" + make_color('a') + " /" + iterator2.next());
                }
            }
            else if (array[2].equalsIgnoreCase("add") && array.length > 3) {
                final StringBuilder sb = new StringBuilder();
                for (int i = 3; i < array.length; ++i) {
                    sb.append(" ");
                    sb.append(array[i]);
                }
                final String substring = sb.toString().substring(1);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added command #" + safeWaypointCommands.get(int8).size());
                this.getSafeWaypointCommands(commandSender).get(int8).add(substring);
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
            else if (array[2].equalsIgnoreCase("get") && array.length > 3) {
                int int9;
                try {
                    int9 = Integer.parseInt(array[3]);
                }
                catch (Exception ex7) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                    return true;
                }
                if (int9 < 0 || int9 >= safeWaypointCommands.get(int8).size()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Command not found");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "/" + safeWaypointCommands.get(int8).get(int9));
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('b') + "The actual command is stored without the preceding slash but still works.");
            }
            else if (array[2].equalsIgnoreCase("remove") && array.length > 3) {
                int int10;
                try {
                    int10 = Integer.parseInt(array[3]);
                }
                catch (Exception ex8) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                    return true;
                }
                if (int10 < 0 || int10 >= safeWaypointCommands.get(int8).size()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Command not found");
                    return true;
                }
                this.getSafeWaypointCommands(commandSender).get(int8).remove(int10);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The command has been removed.");
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
        }
        else if (s2.equalsIgnoreCase("msg")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                return true;
            }
            int int11;
            try {
                int11 = Integer.parseInt(array[1]);
            } catch (NumberFormatException ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                return true;
            }
            final List<String> safeWaypointMessages = this.getSafeWaypointMessages(commandSender);
            if (int11 < 0 || int11 >= safeWaypointMessages.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            if (array.length <= 2) {
                if (safeWaypointMessages.get(int11).length() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "No message has been set for this waypoint.");
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Message: §r" + safeWaypointMessages.get(int11));
                }
                return true;
            }
            if (array[2].equalsIgnoreCase("remove")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been removed.");
                this.getSafeWaypointMessages(commandSender).set(int11, "");
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
            else if (array[2].equalsIgnoreCase("set")) {
                if (array.length > 3) {
                    final StringBuilder sb2 = new StringBuilder();
                    for (int j = 3; j < array.length; ++j) {
                        sb2.append(" ");
                        sb2.append(array[j]);
                    }
                    final String substring2 = sb2.toString().substring(1);
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been set.");
                    this.getSafeWaypointMessages(commandSender).set(int11, substring2);
                    this.clearCache((Player)commandSender);
                    this.clearPathName((Player)commandSender);
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                }
            }
            else if (array[2].equalsIgnoreCase("setcolored")) {
                if (array.length > 3) {
                    final StringBuilder sb3 = new StringBuilder();
                    for (int k = 3; k < array.length; ++k) {
                        sb3.append(" ");
                        sb3.append(array[k]);
                    }
                    final String translateAlternateColorCodes = ChatColor.translateAlternateColorCodes('&', sb3.toString().substring(1));
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been set.");
                    this.getSafeWaypointMessages(commandSender).set(int11, translateAlternateColorCodes);
                    this.clearCache((Player)commandSender);
                    this.clearPathName((Player)commandSender);
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                }
            }
        }
        else if (s2.equalsIgnoreCase("playlist")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist { add path | insert pos path | remove path | list | clear | play | loop }");
                return true;
            }
            String subcmd = array[1];
            Player up = ((Player)commandSender);
            UUID u = up.getUniqueId();
            if (subcmd.equalsIgnoreCase("add")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist add path");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                pl_paths.get(u).add(array[2]);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("remove")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist remove index");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                int k = 0;
                try {
                    k = Integer.parseInt(array[2]);
                    if (k < 0) throw new IllegalArgumentException();
                    if (k >= pl_paths.get(u).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (ArrayIndexOutOfBoundsException ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found in that index.");
                    return true;
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The parameter must be a non-negative integer.");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("insert")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist insert index path");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                int k = 0;
                try {
                    k = Integer.parseInt(array[2]);
                    if (k < 0) throw new IllegalArgumentException();
                    if (k > pl_paths.get(u).size()) k = pl_paths.get(u).size();
                } catch (IllegalArgumentException ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The parameter must be a non-negative integer.");
                    return true;
                }
                pl_paths.get(u).add(k, array[3]);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("list")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "List of paths in your playlist:");
                int n = 0;
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                for (final String path: pl_paths.get(u)) {
                    commandSender.sendMessage("" + make_color('e') + "" + n + "" + make_color('7') + ": " + make_color('f') + "" + path);
                    ++n;
                }
                if (n == 0) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "None at all!");
                }
                return true;
            } else if (subcmd.equalsIgnoreCase("play")) {
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                pl_playing.put(u, true);
                pl_looping.put(u, false);
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.PLAYLIST);

            } else if (subcmd.equalsIgnoreCase("clear")) {
                pl_paths.put(u, new ArrayList<String>());
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Cleared.");
                return true;
            } else if (subcmd.equalsIgnoreCase("loop")) {
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                pl_playing.put(u, true);
                pl_looping.put(u, true);
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.PLAYLIST);
                return true;
            }
        }
        else if (s2.equalsIgnoreCase("insert")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " insert id (speed / n(o effect)) (yaw / c(urrent) / n(o effect)) (pitch / c(urrent) / n(o effect))");
                return true;
            }
            int int12 = Integer.parseInt(array[1]);
            final Location location3 = ((Player)commandSender).getLocation();
            final List<Location> safeWaypoints2 = this.getSafeWaypoints(commandSender);
            if (int12 < 0 || int12 >= safeWaypoints2.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Position must be a non-negative integer.");
                return true;
            }
            if (int12 > safeWaypoints2.size()) {
                int12 = safeWaypoints2.size();
            }
            if (safeWaypoints2.size() > 0 && !safeWaypoints2.get(0).getWorld().getName().equalsIgnoreCase(location3.getWorld().getName())) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot add waypoints to another world!");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            safeWaypoints2.add(int12, location3);
            this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints2);
            final List<Double> safeWaypointSpeeds2 = this.getSafeWaypointSpeeds(commandSender);
            final List<Double> safeWaypointYaw3 = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch3 = this.getSafeWaypointPitch(commandSender);
            double n4 = -1.0;
            if (array.length > 2) {
                try {
                    n4 = Double.parseDouble(array[2]);
                    n4 = Math.abs(n4);
                }
                catch (Exception ex19) {}
            }
            double double3 = -1.0;
            if (array.length > 3) {
                if (array[3].equalsIgnoreCase("c")) {
                    double3 = ((Player)commandSender).getLocation().getYaw();
                }
                else if (array[3].equalsIgnoreCase("n")) {
                    double3 = 444.0;
                }
                else {
                    try {
                        double3 = Double.parseDouble(array[3]);
                    }
                    catch (Exception ex20) {}
                }
            }
            double double4 = -1.0;
            if (array.length > 4) {
                if (array[4].equalsIgnoreCase("c")) {
                    double4 = ((Player)commandSender).getLocation().getPitch();
                }
                else if (array[4].equalsIgnoreCase("n")) {
                    double4 = 444.0;
                }
                else {
                    try {
                        double4 = Double.parseDouble(array[4]);
                    }
                    catch (Exception ex21) {}
                }
            }
            double3 = formatAngleYaw(double3);
            double4 = formatAnglePitch(double4);
            safeWaypointSpeeds2.add(int12, n4);
            safeWaypointYaw3.add(int12, double3);
            safeWaypointPitch3.add(int12, double4);
            UUID u = ((Player)commandSender).getUniqueId();
            this.waypoints_s.put(u, safeWaypointSpeeds2);
            this.waypoints_y.put(u, safeWaypointYaw3);
            this.waypoints_p.put(u, safeWaypointPitch3);
            if (this.waypoints_m.get(u) == null) {
                this.waypoints_m.put(u, new ArrayList<String>());
            }
            this.waypoints_m.get(u).add(int12, "");
            if (this.waypoints_c.get(u) == null) {
                this.waypoints_c.put(u, new ArrayList<List<String>>());
            }
            this.waypoints_c.get(u).add(int12, new ArrayList<String>());
            this.getSafeWaypointDelays(commandSender);
            this.getSafeWaypointOptions(commandSender);
            this.getSafeWaypointInstants(commandSender);
            this.waypoints_l.get(u).add(int12, 0);
            this.waypoints_d.get(u).add(int12, 0.0);
            this.waypoints_i.get(u).add(int12, false);
            if (n4 >= 0.0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted waypoint at #" + int12 + ", setting the speed to " + n4);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted waypoint at #" + int12);
            }
        }
        else if (s2.equalsIgnoreCase("edit")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " edit <id> <speed / n(o effect)> <yaw / c(urrent) / n(o effect)> <pitch / c(urrent) / n(o effect)>");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " (D(o not change) as any parameter will not change it)");
                return true;
            }
            final int int13 = Integer.parseInt(array[1]);
            final List<Location> safeWaypoints3 = this.getSafeWaypoints(commandSender);
            if (int13 < 0 && int13 >= safeWaypoints3.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            final List<Double> safeWaypointSpeeds3 = this.getSafeWaypointSpeeds(commandSender);
            final List<Double> safeWaypointYaw4 = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch4 = this.getSafeWaypointPitch(commandSender);
            double n5 = -1.0;
            if (!array[2].equalsIgnoreCase("D")) {
                try {
                    n5 = Double.parseDouble(array[2]);
                    n5 = Math.abs(n5);
                }
                catch (Exception ex22) {}
            }
            double double5 = -1.0;
            if (!array[3].equalsIgnoreCase("D")) {
                if (array[3].equalsIgnoreCase("c")) {
                    double5 = ((Player)commandSender).getLocation().getYaw();
                }
                else if (array[3].equalsIgnoreCase("n")) {
                    double5 = 444.0;
                }
                else {
                    try {
                        double5 = Double.parseDouble(array[3]);
                    }
                    catch (Exception ex23) {}
                }
            }
            double double6 = -1.0;
            if (!array[4].equalsIgnoreCase("D")) {
                if (array[4].equalsIgnoreCase("c")) {
                    double6 = ((Player)commandSender).getLocation().getPitch();
                }
                else if (array[4].equalsIgnoreCase("n")) {
                    double6 = 444.0;
                }
                else {
                    try {
                        double6 = Double.parseDouble(array[4]);
                    }
                    catch (Exception ex24) {}
                }
            }
            if (!array[2].equalsIgnoreCase("D")) {
                safeWaypointSpeeds3.set(int13, n5);
            }
            if (!array[3].equalsIgnoreCase("D")) {
                safeWaypointYaw4.set(int13, double5);
            }
            if (!array[4].equalsIgnoreCase("D")) {
                safeWaypointPitch4.set(int13, double6);
            }
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds3);
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw4);
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch4);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Edited the properties of waypoint #" + int13);
        }
        else if (s2.equalsIgnoreCase("speed")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                double doubleValue3 = 5.0;
                try {
                    doubleValue3 = this.speed.get(((Player)commandSender).getUniqueId());
                }
                catch (Exception ex25) {}
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "Starting speed is currently " + make_color('a') + "" + doubleValue3);
                return true;
            }
            double double7;
            try {
                double7 = Double.parseDouble(array[1]);
            }
            catch (Exception ex9) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "That is not a number!");
                return true;
            }
            final double abs = Math.abs(double7);
            this.speed.put(((Player)commandSender).getUniqueId(), abs);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "Starting speed set to " + make_color('a') + "" + abs);
        }
        else if (s2.equalsIgnoreCase("fclear")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " fclear player");
                return true;
            }
            final Player player = this.getServer().getPlayer(array[1]);
            if (player == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            this.clearCache(player);
            this.clearPathName((Player)commandSender);
            this.waypoints.put(player.getUniqueId(), new ArrayList<Location>());
            this.waypoints_s.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_y.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_p.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_m.put(player.getUniqueId(), new ArrayList<String>());
            this.waypoints_c.put(player.getUniqueId(), new ArrayList<List<String>>());
            this.getSafeWaypointFlags(player);
            this.waypoints_f.put(player.getUniqueId(), 0);
            this.waypoints_t.put(player.getUniqueId(), -1.0);
            this.waypoints_d.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_l.put(player.getUniqueId(), new ArrayList<Integer>());
            this.waypoints_i.put(player.getUniqueId(), new ArrayList<Boolean>());
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path cleared.");
        }
        else if (s2.equalsIgnoreCase("fstop")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " fstop player");
                return true;
            }
            if (globalMode != null && array[1].equalsIgnoreCase("**")) {
                stopGlobal();
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "OK");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this. If you want to stop global playback: /" + s + " fstop **");
                return true;
            }
            final Player player = this.getServer().getPlayer(array[1]);
            if (player == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Stopped.");
            this.stop(player, PathPlaybackStoppedEvent.StopCause.FSTOP);
        }
        else if (s2.equalsIgnoreCase("fload") || s2.equalsIgnoreCase("fplay")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " " + s2.toLowerCase() + " player path (tp|notp|pathless)");
                return true;
            }
            if (s2.equalsIgnoreCase("fplay") && array[1].equalsIgnoreCase("**")) {
                if (getServer().getOnlinePlayers().size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "No players are online!");
                    return true;
                }
                Player up = commandSender instanceof Player ? ((Player)commandSender) : getServer().getOnlinePlayers().iterator().next();
                UUID u = up.getUniqueId();
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                globalMode = u;
                pl_playing.put(u, true);
                pl_looping.put(u, true);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.FPLAY);
                return true;
            }
            final Player player2 = this.getServer().getPlayer(array[1]);
            if (player2 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.isTrue(this.playing.get(player2.getUniqueId()))) {
                this.stop(player2, PathPlaybackStoppedEvent.StopCause.FSTOP);
            }
            if (array.length > 3) {
                if (array[3].equalsIgnoreCase("tp")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[3].equalsIgnoreCase("notp")) {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[3].equalsIgnoreCase("pathless")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), true);
                }
                else {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
            }
            try {
                final File file = new File(this.paths, array[2]);
                if (!file.isFile()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found");
                    return true;
                }
                final String s3 = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                this.clearCache(player2);
                UUID u2 = player2.getUniqueId();
                this.waypoints.put(u2, new ArrayList<Location>());
                this.waypoints_s.put(u2, new ArrayList<Double>());
                this.waypoints_y.put(u2, new ArrayList<Double>());
                this.waypoints_p.put(u2, new ArrayList<Double>());
                this.waypoints_m.put(u2, new ArrayList<String>());
                this.waypoints_c.put(u2, new ArrayList<List<String>>());
                this.waypoints_f.put(u2, 0);
                this.waypoints_t.put(u2, -1.0);
                this.waypoints_d.put(u2, new ArrayList<Double>());
                this.waypoints_l.put(u2, new ArrayList<Integer>());
                this.waypoints_i.put(u2, new ArrayList<Boolean>());
                final List<Location> safeWaypoints4 = this.getSafeWaypoints(player2);
                final List<Double> safeWaypointSpeeds4 = this.getSafeWaypointSpeeds(player2);
                final List<Double> safeWaypointYaw5 = this.getSafeWaypointYaw(player2);
                final List<Double> safeWaypointPitch5 = this.getSafeWaypointPitch(player2);
                final List<String> safeWaypointMessages2 = this.getSafeWaypointMessages(player2);
                final List<List<String>> safeWaypointCommands2 = this.getSafeWaypointCommands(player2);
                final World world = player2.getWorld();
                final String s4 = s3.split("#")[0];
                final String s5 = s3.split("#")[1];
                final float float1 = Float.parseFloat(s4.split(",")[2]);
                final float float2 = Float.parseFloat(s4.split(",")[3]);
                this.pathnames.put(u2, array[2]);
                int safeFlags = 0;
                if (s4.split(",").length > 4) {
                    safeFlags = Integer.parseInt(s4.split(",")[4]);
                }
                double safeTime = -1;
                if (s4.split(",").length > 5) {
                    safeTime = Double.parseDouble(s4.split(",")[5]);
                }
                int n6 = 0;
                String[] split;
                for (int length = (split = s5.split(Pattern.quote("|"))).length, l = 0; l < length; ++l) {
                    final String s6 = split[l];
                    try {
                        final String[] split2 = s6.split(",");
                        final Location location4 = new Location(world, Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2].split(";")[0]));
                        if (n6 == 0) {
                            location4.setYaw(float1);
                            location4.setPitch(float2);
                        }
                        safeWaypoints4.add(location4);
                        safeWaypointCommands2.add(new ArrayList<String>());
                        if (split2[3].indexOf(10) >= 0) {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3].split("\n")[0]));
                            int n7 = 0;
                            String[] split3;
                            for (int length2 = (split3 = split2[3].split("\n")).length, n8 = 0; n8 < length2; ++n8) {
                                final String s7 = split3[n8];
                                if (n7++ >= 1) {
                                    safeWaypointCommands2.get(n6).add(s7.replace("\uf555", ","));
                                }
                            }
                        }
                        else {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3]));
                        }
                        try {
                            if (split2[2].split(";").length > 3) {
                                this.waypoints_i.get(u2).add(!split2[2].split(";")[3].equalsIgnoreCase("0"));
                            } else {
                                this.waypoints_i.get(u2).add(false);
                            }
                            if (split2[2].split(";").length < 2) {
                                throw new ArrayIndexOutOfBoundsException();
                            }
                            double d = Double.parseDouble(split2[2].split(";")[1]);
                            int lf = Integer.parseInt(split2[2].split(";")[2]);
                            this.waypoints_d.get(u2).add(d);
                            this.waypoints_l.get(u2).add(lf);
                        } catch (Exception ex) {
                            this.waypoints_d.get(u2).add(0.0);
                            this.waypoints_l.get(u2).add(0);
                        }
                        if (split2.length > 4) {
                            final String[] split4 = split2[4].split(":");
                            final String[] split5 = split4[1].split("\\$", 2);
                            if (split5.length > 1) {
                                safeWaypointMessages2.add(split5[1].replace("\uf555", ","));
                            }
                            else {
                                safeWaypointMessages2.add("");
                            }
                            safeWaypointYaw5.add(this.formatAngleYaw(Double.parseDouble(split4[0])));
                            safeWaypointPitch5.add(this.formatAnglePitch(Double.parseDouble(split5[0])));
                        }
                        else {
                            safeWaypointYaw5.add(444.0);
                            safeWaypointPitch5.add(444.0);
                        }
                        ++n6;
                    }
                    catch (Exception ex10) {
                        if (safeWaypointYaw5.size() > safeWaypointPitch5.size()) {
                            safeWaypointYaw5.remove(safeWaypointYaw5.size() - 1);
                        }
                        if (safeWaypointMessages2.size() > safeWaypointYaw5.size()) {
                            safeWaypointMessages2.remove(safeWaypointMessages2.size() - 1);
                        }
                        if (safeWaypointSpeeds4.size() > safeWaypointYaw5.size()) {
                            safeWaypointSpeeds4.remove(safeWaypointSpeeds4.size() - 1);
                        }
                        if (safeWaypoints4.size() > safeWaypointSpeeds4.size()) {
                            safeWaypoints4.remove(safeWaypoints4.size() - 1);
                        }
                        if (safeWaypointCommands2.size() > safeWaypoints4.size()) {
                            safeWaypointCommands2.remove(safeWaypointCommands2.size() - 1);
                        }
                    }
                }
                this.waypoints_y.put(player2.getUniqueId(), safeWaypointYaw5);
                this.waypoints_p.put(player2.getUniqueId(), safeWaypointPitch5);
                this.waypoints_m.put(player2.getUniqueId(), safeWaypointMessages2);
                this.waypoints_c.put(player2.getUniqueId(), safeWaypointCommands2);
                this.waypoints_f.put(player2.getUniqueId(), safeFlags);
                this.waypoints_t.put(player2.getUniqueId(), safeTime);
                this.speed.put(player2.getUniqueId(), Double.parseDouble(s4.split(",")[1]));
                if (!world.getName().equalsIgnoreCase(s4.split(",")[0])) {
                    final World world2 = this.getServer().getWorld(s4.split(",")[0]);
                    if (world2 != null) {
                        final Iterator<Location> iterator3 = safeWaypoints4.iterator();
                        while (iterator3.hasNext()) {
                            iterator3.next().setWorld(world2);
                        }
                        player2.teleport(world2.getSpawnLocation());
                    }
                    else {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s4.split(",")[0] + "'.");
                    }
                }
                this.waypoints.put(player2.getUniqueId(), safeWaypoints4);
                this.waypoints_s.put(player2.getUniqueId(), safeWaypointSpeeds4);
            }
            catch (Exception ex11) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                return true;
            }
            //commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing!");
            pl_playing.put(player2.getUniqueId(), false);
            pl_looping.put(player2.getUniqueId(), false);
            if (s2.equalsIgnoreCase("fplay"))
                this.play(player2, commandSender, true, PathPlaybackStartedEvent.StartCause.FPLAY);
        }
        else if (s2.equalsIgnoreCase("ftplay")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " ftplay player path hours:minutes:seconds (tp|notp|pathless)");
                return true;
            }
            final Player player2 = this.getServer().getPlayer(array[1]);
            if (player2 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.isTrue(this.playing.get(player2.getUniqueId()))) {
                this.stop(player2, PathPlaybackStoppedEvent.StopCause.FSTOP);
            }
            if (array.length > 4) {
                if (array[4].equalsIgnoreCase("tp")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[4].equalsIgnoreCase("notp")) {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[4].equalsIgnoreCase("pathless")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), true);
                }
                else {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
            }

            try {
                final File file = new File(this.paths, array[2]);
                if (!file.isFile()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found");
                    return true;
                }
                final String s3 = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                this.clearCache(player2);
                UUID u2 = player2.getUniqueId();
                this.waypoints.put(u2, new ArrayList<Location>());
                this.waypoints_s.put(u2, new ArrayList<Double>());
                this.waypoints_y.put(u2, new ArrayList<Double>());
                this.waypoints_p.put(u2, new ArrayList<Double>());
                this.waypoints_m.put(u2, new ArrayList<String>());
                this.waypoints_c.put(u2, new ArrayList<List<String>>());
                this.waypoints_f.put(u2, 0);
                this.waypoints_t.put(u2, -1.0);
                this.waypoints_d.put(u2, new ArrayList<Double>());
                this.waypoints_l.put(u2, new ArrayList<Integer>());
                this.waypoints_i.put(u2, new ArrayList<Boolean>());
                final List<Location> safeWaypoints4 = this.getSafeWaypoints(player2);
                final List<Double> safeWaypointSpeeds4 = this.getSafeWaypointSpeeds(player2);
                final List<Double> safeWaypointYaw5 = this.getSafeWaypointYaw(player2);
                final List<Double> safeWaypointPitch5 = this.getSafeWaypointPitch(player2);
                final List<String> safeWaypointMessages2 = this.getSafeWaypointMessages(player2);
                final List<List<String>> safeWaypointCommands2 = this.getSafeWaypointCommands(player2);
                final World world = player2.getWorld();
                final String s4 = s3.split("#")[0];
                final String s5 = s3.split("#")[1];
                final float float1 = Float.parseFloat(s4.split(",")[2]);
                final float float2 = Float.parseFloat(s4.split(",")[3]);
                this.pathnames.put(u2, array[2]);
                int safeFlags = 0;
                if (s4.split(",").length > 4) {
                    safeFlags = Integer.parseInt(s4.split(",")[4]);
                }
                double safeTime = -1;
                if (s4.split(",").length > 5) {
                    safeTime = Double.parseDouble(s4.split(",")[5]);
                }
                int n6 = 0;
                String[] split;
                for (int length = (split = s5.split(Pattern.quote("|"))).length, l = 0; l < length; ++l) {
                    final String s6 = split[l];
                    try {
                        final String[] split2 = s6.split(",");
                        final Location location4 = new Location(world, Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2].split(";")[0]));
                        if (n6 == 0) {
                            location4.setYaw(float1);
                            location4.setPitch(float2);
                        }
                        safeWaypoints4.add(location4);
                        safeWaypointCommands2.add(new ArrayList<String>());
                        if (split2[3].indexOf(10) >= 0) {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3].split("\n")[0]));
                            int n7 = 0;
                            String[] split3;
                            for (int length2 = (split3 = split2[3].split("\n")).length, n8 = 0; n8 < length2; ++n8) {
                                final String s7 = split3[n8];
                                if (n7++ >= 1) {
                                    safeWaypointCommands2.get(n6).add(s7.replace("\uf555", ","));
                                }
                            }
                        }
                        else {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3]));
                        }
                        try {
                            if (split2[2].split(";").length > 3) {
                                this.waypoints_i.get(u2).add(!split2[2].split(";")[3].equalsIgnoreCase("0"));
                            } else {
                                this.waypoints_i.get(u2).add(false);
                            }
                            if (split2[2].split(";").length < 2) {
                                throw new ArrayIndexOutOfBoundsException();
                            }
                            double d = Double.parseDouble(split2[2].split(";")[1]);
                            int lf = Integer.parseInt(split2[2].split(";")[2]);
                            this.waypoints_d.get(u2).add(d);
                            this.waypoints_l.get(u2).add(lf);
                        } catch (Exception ex) {
                            this.waypoints_d.get(u2).add(0.0);
                            this.waypoints_l.get(u2).add(0);
                        }
                        if (split2.length > 4) {
                            final String[] split4 = split2[4].split(":");
                            final String[] split5 = split4[1].split("\\$", 2);
                            if (split5.length > 1) {
                                safeWaypointMessages2.add(split5[1].replace("\uf555", ","));
                            }
                            else {
                                safeWaypointMessages2.add("");
                            }
                            safeWaypointYaw5.add(this.formatAngleYaw(Double.parseDouble(split4[0])));
                            safeWaypointPitch5.add(this.formatAnglePitch(Double.parseDouble(split5[0])));
                        }
                        else {
                            safeWaypointYaw5.add(444.0);
                            safeWaypointPitch5.add(444.0);
                        }
                        ++n6;
                    }
                    catch (Exception ex10) {
                        if (safeWaypointYaw5.size() > safeWaypointPitch5.size()) {
                            safeWaypointYaw5.remove(safeWaypointYaw5.size() - 1);
                        }
                        if (safeWaypointMessages2.size() > safeWaypointYaw5.size()) {
                            safeWaypointMessages2.remove(safeWaypointMessages2.size() - 1);
                        }
                        if (safeWaypointSpeeds4.size() > safeWaypointYaw5.size()) {
                            safeWaypointSpeeds4.remove(safeWaypointSpeeds4.size() - 1);
                        }
                        if (safeWaypoints4.size() > safeWaypointSpeeds4.size()) {
                            safeWaypoints4.remove(safeWaypoints4.size() - 1);
                        }
                        if (safeWaypointCommands2.size() > safeWaypoints4.size()) {
                            safeWaypointCommands2.remove(safeWaypointCommands2.size() - 1);
                        }
                    }
                }
                this.waypoints_y.put(player2.getUniqueId(), safeWaypointYaw5);
                this.waypoints_p.put(player2.getUniqueId(), safeWaypointPitch5);
                this.waypoints_m.put(player2.getUniqueId(), safeWaypointMessages2);
                this.waypoints_c.put(player2.getUniqueId(), safeWaypointCommands2);
                this.waypoints_f.put(player2.getUniqueId(), safeFlags);
                this.waypoints_t.put(player2.getUniqueId(), safeTime);
                this.speed.put(player2.getUniqueId(), Double.parseDouble(s4.split(",")[1]));
                if (!world.getName().equalsIgnoreCase(s4.split(",")[0])) {
                    final World world2 = this.getServer().getWorld(s4.split(",")[0]);
                    if (world2 != null) {
                        final Iterator<Location> iterator3 = safeWaypoints4.iterator();
                        while (iterator3.hasNext()) {
                            iterator3.next().setWorld(world2);
                        }
                        player2.teleport(world2.getSpawnLocation());
                    }
                    else {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s4.split(",")[0] + "'.");
                    }
                }
                this.waypoints.put(player2.getUniqueId(), safeWaypoints4);
                this.waypoints_s.put(player2.getUniqueId(), safeWaypointSpeeds4);
            }
            catch (Exception ex11) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                return true;
            }
            //commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing!");
            String time = array[3];
            if (!waypoints_t.containsKey(player2.getUniqueId())) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            } else if (waypoints_t.get(player2.getUniqueId()) < 0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            }
            double sec = 0;
            try {
                String[] tok = time.split(":");
                if (tok.length > 3 || tok.length < 1) {
                    throw new IllegalArgumentException();
                }
                int x = 1;
                for (int i = tok.length - 1; i >= 0; i--) {
                    sec += (x == 1 ? Double.parseDouble(tok[i]) : Integer.parseInt(tok[i])) * x;
                    x *= 60;
                }
            } catch (Exception ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot parse the given time.");
                return true;
            }
            if (waypoints_t.get(player2.getUniqueId()) > 0) {
                // speed multiplier
                multipl.put(player2.getUniqueId(), waypoints_t.get(player2.getUniqueId()) / sec);
            }
            pl_playing.put(player2.getUniqueId(), false);
            pl_looping.put(player2.getUniqueId(), false);
            this.play(player2, commandSender, true, PathPlaybackStartedEvent.StartCause.FPLAY);
        }
        else if (s2.equalsIgnoreCase("goto")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " goto id");
                return true;
            }
            try {
                final List<Double> safeWaypointYaw6 = this.getSafeWaypointYaw(commandSender);
                final List<Double> safeWaypointPitch6 = this.getSafeWaypointPitch(commandSender);
                final int int14 = Integer.parseInt(array[1]);
                final Location location5 = this.getSafeWaypoints(commandSender).get(int14);
                if (!this.improper(safeWaypointYaw6.get(int14))) {
                    location5.setYaw((float)(double)safeWaypointYaw6.get(int14));
                }
                if (!this.improper(safeWaypointPitch6.get(int14))) {
                    location5.setPitch((float)(double)safeWaypointPitch6.get(int14));
                }
                ((Player)commandSender).teleport(location5);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Teleported.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not teleport!");
            }
        }
        else if (s2.equalsIgnoreCase("remove")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                final List<Location> safeWaypoints5 = this.getSafeWaypoints(commandSender);
                final int n9 = safeWaypoints5.size() - 1;
                if (n9 < 0) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your path is already devoid of waypoints!");
                    return true;
                }
                safeWaypoints5.remove(n9);
                final List<Double> safeWaypointSpeeds5 = this.getSafeWaypointSpeeds(commandSender);
                safeWaypointSpeeds5.remove(n9);
                final List<Double> safeWaypointYaw7 = this.getSafeWaypointYaw(commandSender);
                safeWaypointYaw7.remove(n9);
                final List<Double> safeWaypointPitch7 = this.getSafeWaypointPitch(commandSender);
                safeWaypointPitch7.remove(n9);
                this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints5);
                this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds5);
                this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw7);
                this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch7);
                if (this.waypoints_m.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
                }
                this.waypoints_m.get(((Player)commandSender).getUniqueId()).remove(n9);
                if (this.waypoints_c.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
                }
                this.waypoints_c.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_d.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_l.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_i.get(((Player)commandSender).getUniqueId()).remove(n9);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed last point!");
            }
            try {
                final int int15 = Integer.parseInt(array[1]);
                final List<Location> safeWaypoints6 = this.getSafeWaypoints(commandSender);
                safeWaypoints6.remove(int15);
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
                final List<Double> safeWaypointSpeeds6 = this.getSafeWaypointSpeeds(commandSender);
                safeWaypointSpeeds6.remove(int15);
                final List<Double> safeWaypointYaw8 = this.getSafeWaypointYaw(commandSender);
                safeWaypointYaw8.remove(int15);
                final List<Double> safeWaypointPitch8 = this.getSafeWaypointPitch(commandSender);
                safeWaypointPitch8.remove(int15);
                this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints6);
                this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds6);
                this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw8);
                this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch8);
                if (this.waypoints_m.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
                }
                this.waypoints_m.get(((Player)commandSender).getUniqueId()).remove(int15);
                if (this.waypoints_c.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
                }
                this.waypoints_c.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_d.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_l.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_i.get(((Player)commandSender).getUniqueId()).remove(int15);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed point #" + int15 + "!");
            }
            catch (NumberFormatException ex12) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex13) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
            }
            catch (Exception ex14) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not remove!");
            }
        }
        else if (s2.equalsIgnoreCase("clone")) {

            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " clone player");
                return true;
            }
            final Player player3 = this.getServer().getPlayer(array[1]);
            if (player3 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.speed.containsKey(player3.getUniqueId())) {
                this.speed.put(((Player)commandSender).getUniqueId(), this.speed.get(player3.getUniqueId()));
            }
            else {
                this.speed.remove(((Player)commandSender).getUniqueId());
            }
            this.clearCache((Player)commandSender);
            this.pathnames.put(((Player)commandSender).getUniqueId(), this.pathnames.get((((Player)commandSender).getUniqueId())));
            if (this.pathnames.get(((Player)commandSender).getUniqueId()) != null)
                this.clearPathName((Player)commandSender);
            this.waypoints.put(((Player)commandSender).getUniqueId(), this.getSafeWaypoints(player3));
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointSpeeds(player3));
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointYaw(player3));
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointPitch(player3));
            this.waypoints_m.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointMessages(player3));
            this.waypoints_c.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointCommands(player3));
            this.waypoints_d.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointDelays(player3));
            this.waypoints_l.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointOptions(player3));
            this.waypoints_i.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointInstants(player3));
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Path cloned.");
        }
        else if (s2.equalsIgnoreCase("load")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                final StringBuilder sb4 = new StringBuilder();
                File[] listFiles;
                for (int length3 = (listFiles = this.paths.listFiles()).length, n10 = 0; n10 < length3; ++n10) {
                    final File file2 = listFiles[n10];
                    if (file2.isFile()) {
                        sb4.append(", ");
                        sb4.append(file2.getName());
                    }
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "List of saved paths:");
                if (sb4.length() < 1) {
                    commandSender.sendMessage("" + make_color('c') + "No paths were found");
                    return true;
                }
                commandSender.sendMessage(sb4.toString().substring(2));
                return true;
            }
            else {
                try {
                    final File file3 = new File(this.paths, array[1]);
                    if (!file3.isFile()) {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Not found");
                        return true;
                    }
                    final String s8 = new String(Files.readAllBytes(file3.toPath()), StandardCharsets.UTF_8);
                    final Player player4 = (Player)commandSender;
                    final UUID u4 = player4.getUniqueId();
                    int n11 = 0;
                    this.clearCache((Player)commandSender);
                    this.waypoints.put(player4.getUniqueId(), new ArrayList<Location>());
                    this.waypoints_s.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_y.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_p.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_m.put(player4.getUniqueId(), new ArrayList<String>());
                    this.waypoints_c.put(player4.getUniqueId(), new ArrayList<List<String>>());
                    this.waypoints_f.put(player4.getUniqueId(), 0);
                    this.waypoints_t.put(player4.getUniqueId(), -1.0);
                    this.waypoints_d.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_l.put(player4.getUniqueId(), new ArrayList<Integer>());
                    this.waypoints_i.put(player4.getUniqueId(), new ArrayList<Boolean>());
                    final List<Location> safeWaypoints7 = this.getSafeWaypoints(player4);
                    final List<Double> safeWaypointSpeeds7 = this.getSafeWaypointSpeeds(player4);
                    final List<Double> safeWaypointYaw9 = this.getSafeWaypointYaw(player4);
                    final List<Double> safeWaypointPitch9 = this.getSafeWaypointPitch(player4);
                    final List<String> safeWaypointMessages3 = this.getSafeWaypointMessages(player4);
                    final List<List<String>> safeWaypointCommands3 = this.getSafeWaypointCommands(player4);
                    final World world3 = player4.getWorld();
                    final String s9 = s8.split("#")[0];
                    final String s10 = s8.split("#")[1];
                    final float float3 = Float.parseFloat(s9.split(",")[2]);
                    final float float4 = Float.parseFloat(s9.split(",")[3]);
                    this.pathnames.put(u4, array[1]);
                    int n12 = 0;
                    int safeFlags2 = 0;
                    if (s9.split(",").length > 4) {
                        safeFlags2 = Integer.parseInt(s9.split(",")[4]);
                    }
                    double safeTime = -1.0;
                    if (s9.split(",").length > 5) {
                        safeTime = Double.parseDouble(s9.split(",")[5]);
                    }
                    String[] split6;
                    for (int length4 = (split6 = s10.split(Pattern.quote("|"))).length, n13 = 0; n13 < length4; ++n13) {
                        final String s11 = split6[n13];
                        try {
                            final String[] split7 = s11.split(",");
                            final Location location6 = new Location(world3, Double.parseDouble(split7[0]), Double.parseDouble(split7[1]), Double.parseDouble(split7[2].split(";")[0]));
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
                                    this.waypoints_i.get(u4).add(!split7[2].split(";")[3].equalsIgnoreCase("0"));
                                } else {
                                    this.waypoints_i.get(u4).add(false);
                                }
                                if (split7[2].split(";").length < 2) {
                                    throw new ArrayIndexOutOfBoundsException();
                                }
                                double d = Double.parseDouble(split7[2].split(";")[1]);
                                int lf = Integer.parseInt(split7[2].split(";")[2]);
                                this.waypoints_d.get(u4).add(d);
                                this.waypoints_l.get(u4).add(lf);
                            } catch (Exception ex) {
                                this.waypoints_d.get(u4).add(0.0);
                                this.waypoints_l.get(u4).add(0);
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
                    this.waypoints.put(player4.getUniqueId(), safeWaypoints7);
                    this.waypoints_s.put(player4.getUniqueId(), safeWaypointSpeeds7);
                    this.waypoints_y.put(player4.getUniqueId(), safeWaypointYaw9);
                    this.waypoints_p.put(player4.getUniqueId(), safeWaypointPitch9);
                    this.waypoints_m.put(player4.getUniqueId(), safeWaypointMessages3);
                    this.waypoints_c.put(player4.getUniqueId(), safeWaypointCommands3);
                    this.waypoints_f.put(player4.getUniqueId(), safeFlags2);
                    this.waypoints_t.put(player4.getUniqueId(), safeTime);
                    this.speed.put(player4.getUniqueId(), Double.parseDouble(s9.split(",")[1]));
                    if (!world3.getName().equalsIgnoreCase(s9.split(",")[0])) {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s9.split(",")[0] + "'.");
                    }
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Successfully loaded!");
                    if (n11 > 0)
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Skipped " + n11 + " malformed entries");
                }
                catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "" + ex.toString());
                }
            }
        }
        else if (s2.equalsIgnoreCase("save")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " save filename");
                return true;
            }
            final String s13 = array[1];
            if (s13.contains(".")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not save");
                return true;
            }
            final StringBuilder sb5 = new StringBuilder();
            boolean b2 = true;
            final Player player5 = (Player)commandSender;
            final List<Location> safeWaypoints8 = this.getSafeWaypoints(player5);
            final List<Double> safeWaypointSpeeds8 = this.getSafeWaypointSpeeds(player5);
            final List<Double> safeWaypointYaw10 = this.getSafeWaypointYaw(player5);
            final List<Double> safeWaypointPitch10 = this.getSafeWaypointPitch(player5);
            final List<String> safeWaypointMessages4 = this.getSafeWaypointMessages(player5);
            final List<List<String>> safeWaypointCommands4 = this.getSafeWaypointCommands(player5);
            sb5.append(player5.getWorld().getName());
            sb5.append(",");
            Double value = this.speed.get(player5.getUniqueId());
            if (value == null) {
                value = 5.0;
            }
            sb5.append(value);
            sb5.append(",");
            float yaw = 0.0f;
            float pitch = 0.0f;
            if (safeWaypoints8.size() > 0) {
                yaw = safeWaypoints8.get(0).getYaw();
                pitch = safeWaypoints8.get(0).getPitch();
            }
            sb5.append(yaw);
            sb5.append(",");
            sb5.append(pitch);
            sb5.append(",");
            sb5.append(this.getSafeWaypointFlags(player5));
            sb5.append(",");
            UUID u4 = player5.getUniqueId();
            sb5.append(waypoints_t.containsKey(u4) ? waypoints_t.get(u4) : -1.0);
            sb5.append("#");
            for (int n16 = 0; n16 < safeWaypoints8.size(); ++n16) {
                if (b2) {
                    b2 = !b2;
                }
                else {
                    sb5.append("|");
                }
                final Location location7 = safeWaypoints8.get(n16);
                sb5.append(location7.getX());
                sb5.append(",");
                sb5.append(location7.getY());
                sb5.append(",");
                sb5.append(location7.getZ());
                sb5.append(";");
                sb5.append(waypoints_d.get(u4).get(n16));
                sb5.append(";");
                sb5.append(waypoints_l.get(u4).get(n16));
                sb5.append(";");
                sb5.append(waypoints_i.get(u4).get(n16) ? "1" : "0");
                sb5.append(",");
                sb5.append(safeWaypointSpeeds8.get(n16));
                if (safeWaypointCommands4.get(n16).size() > 0) {
                    for (final String s14 : safeWaypointCommands4.get(n16)) {
                        sb5.append("\n");
                        sb5.append(s14.replace(",", "\uf555"));
                    }
                }
                sb5.append(",");
                sb5.append(safeWaypointYaw10.get(n16) + ":" + safeWaypointPitch10.get(n16));
                sb5.append("$" + safeWaypointMessages4.get(n16).replace(",", "\uf555"));
            }
            try {
                final PrintWriter printWriter = new PrintWriter(new File(this.paths, s13), "UTF-8");
                printWriter.print(sb5.toString());
                printWriter.close();
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Saved: open with /" + s + " load " + s13);
                this.pathnames.put(u4, s13);
            }
            catch (Exception ex2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "" + ex2.toString());
            }
        }
        else if (s2.equalsIgnoreCase("clear")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            this.waypoints.put(((Player)commandSender).getUniqueId(), new ArrayList<Location>());
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
            this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
            this.getSafeWaypointFlags((Player)commandSender);
            this.waypoints_f.put(((Player)commandSender).getUniqueId(), 0);
            this.waypoints_t.put(((Player)commandSender).getUniqueId(), -1.0);
            this.waypoints_d.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_l.put(((Player)commandSender).getUniqueId(), new ArrayList<Integer>());
            this.waypoints_i.put(((Player)commandSender).getUniqueId(), new ArrayList<Boolean>());
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path cleared.");
        }
        else if (s2.equalsIgnoreCase("resume")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId2 = ((Player)commandSender).getUniqueId();
            if (!this.playing.containsKey(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.playing.get(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.paused.containsKey(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not paused!");
                return true;
            }
            if (!this.paused.get(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not paused!");
                return true;
            }
            this.paused.put(uniqueId2, false);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Resuming...");
        }
        else if (s2.equalsIgnoreCase("pause")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final Player player6 = (Player)commandSender;
            final UUID uniqueId3 = player6.getUniqueId();
            if (!this.playing.containsKey(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.playing.get(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (this.paused.containsKey(uniqueId3) && this.paused.get(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're already paused!");
                return true;
            }
            player6.setVelocity(new Vector(0.0, 0.0, 0.0));
            this.paused.put(uniqueId3, true);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Paused: continue with /" + s + " resume");
        }
        else if (s2.equalsIgnoreCase("play")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Only players can run this.");
                return true;
            }
            if (this.isTrue(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Already playing!");
                return true;
            }
            final Player player7 = (Player)commandSender;
            pl_playing.put(player7.getUniqueId(), false);
            pl_looping.put(player7.getUniqueId(), false);
            player7.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
            this.play(player7, commandSender, false, PathPlaybackStartedEvent.StartCause.MANUAL);
        }
        else if (s2.equalsIgnoreCase("tplay")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " tplay ((hours:)minutes:)seconds");
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Only players can run this.");
                return true;
            }
            if (this.isTrue(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Already playing!");
                return true;
            }
            String time = array[1];
            final Player player7 = (Player)commandSender;
            UUID u = player7.getUniqueId();
            if (!waypoints_t.containsKey(u)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            } else if (waypoints_t.get(u) < 0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            }
            double sec = 0;
            try {
                String[] tok = time.split(":");
                if (tok.length > 3 || tok.length < 1) {
                    throw new IllegalArgumentException();
                }
                int x = 1;
                for (int i = tok.length - 1; i >= 0; i--) {
                    sec += (x == 1 ? Double.parseDouble(tok[i]) : Integer.parseInt(tok[i])) * x;
                    x *= 60;
                }
            } catch (Exception ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot parse the given time.");
                return true;
            }
            if (waypoints_t.get(u) > 0) {
                multipl.put(u, waypoints_t.get(u) / sec);
            }
            pl_playing.put(player7.getUniqueId(), false);
            pl_looping.put(player7.getUniqueId(), false);
            player7.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
            this.play(player7, commandSender, false, PathPlaybackStartedEvent.StartCause.MANUAL);
        }
        else if (s2.equalsIgnoreCase("stop")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this." + (commandSender.hasPermission("servercinematics.edit") ? " If you want to stop global playback: /" + s + " fstop **" : ""));
                return true;
            }
            if (this.isFalse(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You are not playing!");
                return true;
            }
            this.stop((Player)commandSender, PathPlaybackStoppedEvent.StopCause.MANUAL);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Stopped.");
        }
        else {
            this.sendMultilineMessage(commandSender, helpString.replace("###", s), "" + make_color('7') + "");
        }
        return true;
    }
    private static final String helpString = "" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics " + make_color('f') + "by " + make_color('f') + "CosmoConsole, Kisko, Pietu1998\n/### help - Display this message\n/### add (speed) (yaw/N/C) (pitch/N/C) - Add waypoint to your present location, as last waypoint to your path (and possibly make it change speed)\n/### insert id (speed) (yaw/N/C) (pitch/N/C) - Add waypoint to your present location, to your path at a specific position (and possibly make it change speed)\n/### edit id <speed / d(on't change)> [yaw/N/C/d] [pitch/N/C/d] - Edit properties of a waypoint\n/### clone player - Clone someone elses path\n/### msg id {set msg | setcolored msg | remove} - Set/remove a message to a waypoint\n/### cmd id {add | list | get | remove} - See/add/remove commands of a waypoint\n/### option (id) (option) - See all possible options for a waypoint or toggle one of them\n/### delay id delay - See, add or remove a waypoint delay\n/### flags - See all possible flags.\n/### flag id - Toggles a flag for the path.\n/### list - List waypoints in your path\n/### playlist {add | list | insert | remove | clear | play | loop} - See/add/remove commands of a waypoint\n/### goto id - Teleports to a waypoint\n/### remove (id) - Remove a waypoint from your path (default: last one)\n/### clear - Clear your path\n/### load (file) - List saved paths or load one\n/### save file - Save the current path to a file\n/### speed (speed) - Get / set flying speed\n/### play - Play your path\n/### tplay ((hours:)minutes:)seconds - Play your path with specific duration\n/### tpmode - Toggle tpmode (tpmode has pitch & yaw support but is less smooth)\n/### pathless - Toggle pathless mode (automatic tpmode + teleports to waypoints only)\n/### fplay player path (tp | notp | pathless) - Force the player to load and play a path (possibly in tpmode)\n/### ftplay player ((hh:)mm:)ss path (tp | notp | pathless) - Force the player to load and play a path with specific timespan (possibly in tpmode)\n/### fstop player - Force the player to stop the current path\n/### fclear player - Clear the path of another player\n/### pause - Pause the current path\n/### resume - Resume from last pause\n/### stop - Stop playing\n/### reload - Reload configuration";

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] array) {
        if (!command.getName().equalsIgnoreCase("camera")) {
            return false;
        }
        if (array.length < 1) {
            commandSender.sendMessage("" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics " + make_color('f') + "by " + make_color('f') + "CosmoConsole, Kisko, Pietu1998, " + make_color('a') + "version " + this.getDescription().getVersion());
            commandSender.sendMessage("" + make_color('7') + "See /cam help for help.");
            return true;
        }
        if ((!array[0].startsWith("f") || array[0].equalsIgnoreCase("flag")) && !(commandSender instanceof Player)) {
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Only players can use.");
            return true;
        }
        if (commandSender instanceof Player) {
            this.getSafeWaypointDelays(commandSender);
            this.getSafeWaypointOptions(commandSender);
            this.getSafeWaypointInstants(commandSender);
        }
        final String s2 = array[0];
        if (s2.equalsIgnoreCase("help")) {
            this.sendMultilineMessage(commandSender, helpString.replace("###", s), "" + make_color('7') + "");
        }
        else if (s2.equalsIgnoreCase("list")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "List of waypoints in your path:");
            int n = 0;
            final List<Double> safeWaypointYaw = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch = this.getSafeWaypointPitch(commandSender);
            for (final Location location : this.getSafeWaypoints(commandSender)) {
                final double doubleValue = safeWaypointYaw.get(n);
                final double doubleValue2 = safeWaypointPitch.get(n);
                commandSender.sendMessage("" + make_color('e') + "" + n + "" + make_color('7') + ": " + make_color('f') + "" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ":" + ((doubleValue > 400.0) ? "-" : String.format(Locale.ENGLISH, "%.1f", doubleValue)) + "," + ((doubleValue2 > 400.0) ? "-" : String.format(Locale.ENGLISH, "%.1f", doubleValue2)));
                ++n;
            }
            if (n == 0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "None at all!");
            }
        }
        else if (s2.equalsIgnoreCase("delay")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " delay id delaylength");
                return true;
            }
            if (array.length < 3) {
                int b = 0;
                final Player p = (Player)commandSender;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Current delay: " + String.format("%.2f", this.waypoints_d.get(p.getUniqueId()).get(b)));
                return true;
            }
            try {
                final double int7 = Double.parseDouble(array[2]);
                final Player p = (Player)commandSender;
                int b = 0;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                if (int7 < 0) {
                    throw new IndexOutOfBoundsException();
                }
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
                this.waypoints_d.get(p.getUniqueId()).set(b, int7);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Delay set.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid delay!");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
            }
        }
        else if (s2.equalsIgnoreCase("instant")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " instant id");
                return true;
            }
            try {
                final Player p = (Player)commandSender;
                int b = 0;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                this.clearCache((Player)commandSender);
                boolean newFlag = !this.waypoints_i.get(p.getUniqueId()).get(b);
                this.waypoints_i.get(p.getUniqueId()).set(b, newFlag);
                if (newFlag)
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Point is now instant. (Only works in tpmode)");
                else
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Point is no longer instant.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid point!");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
            }
        }
        else if (s2.equalsIgnoreCase("option")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " option id option_id");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "0 " + make_color('6') + "- " + make_color('f') + "Teleport player immediately to the next point from this waypoint after the delay (if tpmode enabled)");
            }
            if (array.length < 3) {
                if (commandSender instanceof Player) {
                    int b = 0;
                    final Player p = (Player)commandSender;
                    try {
                        b = Integer.parseInt(array[1]);
                        if (b < 0) throw new ArrayIndexOutOfBoundsException();
                        if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                    } catch (Exception ex) {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                        return true;
                    }
                    int o = this.waypoints_l.get(p.getUniqueId()).get(b);
                    StringBuilder sb = new StringBuilder();
                    int n = 0;
                    for (int a = 1; a >= 0; a <<= 1) {
                        if ((o&a)>0) {
                            sb.append(n);
                            sb.append(" ");
                        }
                        n++;
                    }
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Currently enabled options: " + sb.toString());
                }
                return true;
            }
            try {
                final int int7 = Integer.parseInt(array[2]);
                final Player p = (Player)commandSender;
                int b = 0;
                try {
                    b = Integer.parseInt(array[1]);
                    if (b < 0) throw new ArrayIndexOutOfBoundsException();
                    if (b >= this.waypoints_l.get(p.getUniqueId()).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Invalid path ID!");
                    return true;
                }
                if (int7 < 0 || int7 > 0) {
                    throw new IndexOutOfBoundsException();
                }
                final int o = this.waypoints_l.get(p.getUniqueId()).get(b) & 1 << int7;
                final boolean oldState = o != 0;
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
                this.waypoints_l.get(p.getUniqueId()).set(b, o ^ 1 << int7);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Option " + int7 + " is now " + (oldState ? "" + make_color('c') + "OFF" : "" + make_color('a') + "ON") + "" + make_color('e') + ".");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Option not found.");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle option!");
            }
        }
        else if (s2.equalsIgnoreCase("flags")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "0 " + make_color('6') + "- " + make_color('f') + "Teleport player to first waypoint after path finishes");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "1 " + make_color('6') + "- " + make_color('f') + "Teleport player to original location after path finishes");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "2 " + make_color('6') + "- " + make_color('f') + "Allow player to turn during delay (automatically if not tpmode)");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "3 " + make_color('6') + "- " + make_color('f') + "Smooth velocity from standstill when starting path");
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "4 " + make_color('6') + "- " + make_color('f') + "Smooth velocity from standstill when ending path");
        }
        else if (s2.equalsIgnoreCase("flag")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " flag flag_id");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "See all possible flags with " + make_color('c') + "/" + s + " flags");
                if (commandSender instanceof Player) {
                    final Player p = (Player)commandSender;
                    int o = this.getSafeWaypointFlags(p);
                    StringBuilder sb = new StringBuilder();
                    int n = 0;
                    for (int a = 1; a >= 0; a <<= 1) {
                        if ((o&a)>0) {
                            sb.append(n);
                            sb.append(" ");
                        }
                        n++;
                    }
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Currently enabled flags: " + sb.toString());
                }
                return true;
            }
            try {
                final int int7 = Integer.parseInt(array[1]);
                final Player p = (Player)commandSender;
                if (int7 < 0 || int7 > 4) {
                    throw new IndexOutOfBoundsException();
                }
                final int o = this.getSafeWaypointFlags(p) & (1 << int7);
                final boolean oldState = o != 0;
                this.waypoints_t.put(p.getUniqueId(), -1.0);
                this.waypoints_f.put(p.getUniqueId(), this.getSafeWaypointFlags(p) ^ (1 << int7));
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Flag " + int7 + " is now " + (oldState ? "" + make_color('c') + "OFF" : "" + make_color('a') + "ON") + "" + make_color('e') + ".");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Flag not found.");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not toggle flag!");
            }
        }
        else if (s2.equalsIgnoreCase("reload")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            this.reloadConfig();
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Reloaded.");
        }
        else if (s2.equalsIgnoreCase("add")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final Location location2 = ((Player)commandSender).getLocation();
            final List<Location> safeWaypoints = this.getSafeWaypoints(commandSender);
            if (safeWaypoints.size() > 0 && !safeWaypoints.get(0).getWorld().getName().equalsIgnoreCase(location2.getWorld().getName())) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot add waypoints to another world!");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            safeWaypoints.add(location2);
            this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints);
            final List<Double> safeWaypointSpeeds = this.getSafeWaypointSpeeds(commandSender);
            final List<Double> safeWaypointYaw2 = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch2 = this.getSafeWaypointPitch(commandSender);
            double n2 = -1.0;
            if (array.length > 1) {
                try {
                    n2 = Double.parseDouble(array[1]);
                    n2 = Math.abs(n2);
                }
                catch (Exception ex16) {}
            }
            double double1 = 444.0;
            if (array.length > 2) {
                if (array[2].equalsIgnoreCase("c")) {
                    double1 = ((Player)commandSender).getLocation().getYaw();
                }
                else if (array[2].equalsIgnoreCase("n")) {
                    double1 = 444.0;
                }
                else {
                    try {
                        double1 = Double.parseDouble(array[2]);
                    }
                    catch (Exception ex17) {}
                }
            }
            else {
                double1 = ((Player)commandSender).getLocation().getYaw();
            }
            double double2 = 444.0;
            if (array.length > 3) {
                if (array[3].equalsIgnoreCase("c")) {
                    double2 = ((Player)commandSender).getLocation().getPitch();
                }
                else if (array[3].equalsIgnoreCase("n")) {
                    double2 = 444.0;
                }
                else {
                    try {
                        double2 = Double.parseDouble(array[3]);
                    }
                    catch (Exception ex18) {}
                }
            }
            else {
                double2 = ((Player)commandSender).getLocation().getPitch();
            }
            double1 = formatAngleYaw(double1);
            double2 = formatAnglePitch(double2);
            safeWaypointSpeeds.add(n2);
            safeWaypointYaw2.add(double1);
            safeWaypointPitch2.add(double2);
            UUID u = ((Player)commandSender).getUniqueId();
            this.waypoints_s.put(u, safeWaypointSpeeds);
            this.waypoints_y.put(u, safeWaypointYaw2);
            this.waypoints_p.put(u, safeWaypointPitch2);
            if (this.waypoints_m.get(u) == null) {
                this.waypoints_m.put(u, new ArrayList<String>());
            }
            this.waypoints_m.get(u).add("");
            if (this.waypoints_c.get(u) == null) {
                this.waypoints_c.put(u, new ArrayList<List<String>>());
            }
            this.waypoints_c.get(u).add(new ArrayList<String>());
            this.getSafeWaypointDelays(commandSender);
            this.getSafeWaypointOptions(commandSender);
            this.getSafeWaypointInstants(commandSender);
            this.waypoints_l.get(u).add(0);
            this.waypoints_d.get(u).add(0.0);
            this.waypoints_i.get(u).add(false);
            if (n2 >= 0.0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added waypoint #" + (safeWaypoints.size() - 1) + ", setting the speed to " + n2);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added waypoint #" + (safeWaypoints.size() - 1));
            }
        }
        else {
            return onCommand_2(commandSender, command, s, array);
        }
        return true;
    }

    private boolean onCommand_2(final CommandSender commandSender, final Command command, final String s, final String[] array)
    {
        final String s2 = array[0];
        if (s2.equalsIgnoreCase("tpmode")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId = ((Player)commandSender).getUniqueId();
            if (this.pathless.containsKey(uniqueId) && this.pathless.get(uniqueId)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please disable pathless mode first.");
                return true;
            }
            final Boolean b = this.teleport.get(uniqueId);
            this.clearCache(uniqueId);
            if (this.teleport.containsKey(uniqueId) && b) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Teleport mode switched off for next path (yaw/pitch won't work, more smooth)");
                this.teleport.put(uniqueId, false);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Teleport mode switched on for next path (yaw/pitch will work, less smooth)");
                this.teleport.put(uniqueId, true);
            }
            return true;
        } else if (s2.equalsIgnoreCase("pathless")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId = ((Player)commandSender).getUniqueId();
            final Boolean b = this.pathless.get(uniqueId);
            this.clearCache(((Player)commandSender));
            if (this.pathless.containsKey(uniqueId) && b) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Pathless switched off (tpmode configurable)");
                this.pathless.put(uniqueId, false);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Pathless switched on (automatic tpmode)");
                this.pathless.put(uniqueId, true);
                this.teleport.put(uniqueId, true);
            }
            return true;
        } else if (s2.equalsIgnoreCase("cmd")) {
            if (!commandSender.hasPermission("servercinematics.edit") || !commandSender.hasPermission("servercinematics.cmd")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " cmd id { list | add commandwithout/ | get index | remove index }");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "%player% will be replaced with the player's name");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "command starting with a ~ will be run exactly as if the player");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "    him- or herself had typed it, without the starting ~ of course.");
                return true;
            }
            int int8;
            try {
                int8 = Integer.parseInt(array[1]);
            }
            catch (Exception ex6) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                return true;
            }
            final List<List<String>> safeWaypointCommands = this.getSafeWaypointCommands(commandSender);
            if (int8 < 0 || int8 >= safeWaypointCommands.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            if (array.length <= 2) {
                return true;
            }
            if (array[2].equalsIgnoreCase("list")) {
                int n3 = 0;
                final Iterator<String> iterator2 = safeWaypointCommands.get(int8).iterator();
                while (iterator2.hasNext()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "" + n3++ + " " + make_color('f') + "-" + make_color('a') + " /" + iterator2.next());
                }
            }
            else if (array[2].equalsIgnoreCase("add") && array.length > 3) {
                final StringBuilder sb = new StringBuilder();
                for (int i = 3; i < array.length; ++i) {
                    sb.append(" ");
                    sb.append(array[i]);
                }
                final String substring = sb.toString().substring(1);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added command #" + safeWaypointCommands.get(int8).size());
                this.getSafeWaypointCommands(commandSender).get(int8).add(substring);
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
            else if (array[2].equalsIgnoreCase("get") && array.length > 3) {
                int int9;
                try {
                    int9 = Integer.parseInt(array[3]);
                }
                catch (Exception ex7) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                    return true;
                }
                if (int9 < 0 || int9 >= safeWaypointCommands.get(int8).size()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Command not found");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "/" + safeWaypointCommands.get(int8).get(int9));
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('b') + "The actual command is stored without the preceding slash but still works.");
            }
            else if (array[2].equalsIgnoreCase("remove") && array.length > 3) {
                int int10;
                try {
                    int10 = Integer.parseInt(array[3]);
                }
                catch (Exception ex8) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Improper number");
                    return true;
                }
                if (int10 < 0 || int10 >= safeWaypointCommands.get(int8).size()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Command not found");
                    return true;
                }
                this.getSafeWaypointCommands(commandSender).get(int8).remove(int10);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The command has been removed.");
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
        }
        else if (s2.equalsIgnoreCase("msg")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                return true;
            }
            int int11;
            try {
                int11 = Integer.parseInt(array[1]);
            } catch (NumberFormatException ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                return true;
            }
            final List<String> safeWaypointMessages = this.getSafeWaypointMessages(commandSender);
            if (int11 < 0 || int11 >= safeWaypointMessages.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            if (array.length <= 2) {
                if (safeWaypointMessages.get(int11).length() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "No message has been set for this waypoint.");
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Message: §r" + safeWaypointMessages.get(int11));
                }
                return true;
            }
            if (array[2].equalsIgnoreCase("remove")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been removed.");
                this.getSafeWaypointMessages(commandSender).set(int11, "");
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
            }
            else if (array[2].equalsIgnoreCase("set")) {
                if (array.length > 3) {
                    final StringBuilder sb2 = new StringBuilder();
                    for (int j = 3; j < array.length; ++j) {
                        sb2.append(" ");
                        sb2.append(array[j]);
                    }
                    final String substring2 = sb2.toString().substring(1);
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been set.");
                    this.getSafeWaypointMessages(commandSender).set(int11, substring2);
                    this.clearCache((Player)commandSender);
                    this.clearPathName((Player)commandSender);
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                }
            }
            else if (array[2].equalsIgnoreCase("setcolored")) {
                if (array.length > 3) {
                    final StringBuilder sb3 = new StringBuilder();
                    for (int k = 3; k < array.length; ++k) {
                        sb3.append(" ");
                        sb3.append(array[k]);
                    }
                    final String translateAlternateColorCodes = ChatColor.translateAlternateColorCodes('&', sb3.toString().substring(1));
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "The message of this waypoint has been set.");
                    this.getSafeWaypointMessages(commandSender).set(int11, translateAlternateColorCodes);
                    this.clearCache((Player)commandSender);
                    this.clearPathName((Player)commandSender);
                }
                else {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " msg id { set msg | setcolored msg | remove }");
                }
            }
        }
        else if (s2.equalsIgnoreCase("playlist")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist { add path | insert pos path | remove path | list | clear | play | loop }");
                return true;
            }
            String subcmd = array[1];
            Player up = ((Player)commandSender);
            UUID u = up.getUniqueId();
            if (subcmd.equalsIgnoreCase("add")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist add path");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                pl_paths.get(u).add(array[2]);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Added path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("remove")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist remove index");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                int k = 0;
                try {
                    k = Integer.parseInt(array[2]);
                    if (k < 0) throw new IllegalArgumentException();
                    if (k >= pl_paths.get(u).size()) throw new ArrayIndexOutOfBoundsException();
                } catch (ArrayIndexOutOfBoundsException ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found in that index.");
                    return true;
                } catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The parameter must be a non-negative integer.");
                    return true;
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("insert")) {
                if (array.length < 2) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " playlist insert index path");
                    return true;
                }
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                int k = 0;
                try {
                    k = Integer.parseInt(array[2]);
                    if (k < 0) throw new IllegalArgumentException();
                    if (k > pl_paths.get(u).size()) k = pl_paths.get(u).size();
                } catch (IllegalArgumentException ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The parameter must be a non-negative integer.");
                    return true;
                }
                pl_paths.get(u).add(k, array[3]);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted path.");
                return true;
            } else if (subcmd.equalsIgnoreCase("list")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "List of paths in your playlist:");
                int n = 0;
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                for (final String path: pl_paths.get(u)) {
                    commandSender.sendMessage("" + make_color('e') + "" + n + "" + make_color('7') + ": " + make_color('f') + "" + path);
                    ++n;
                }
                if (n == 0) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "None at all!");
                }
                return true;
            } else if (subcmd.equalsIgnoreCase("play")) {
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                pl_playing.put(u, true);
                pl_looping.put(u, false);
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.PLAYLIST);

            } else if (subcmd.equalsIgnoreCase("clear")) {
                pl_paths.put(u, new ArrayList<String>());
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Cleared.");
                return true;
            } else if (subcmd.equalsIgnoreCase("loop")) {
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                pl_playing.put(u, true);
                pl_looping.put(u, true);
                up.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.PLAYLIST);
                return true;
            }
        }
        else if (s2.equalsIgnoreCase("insert")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " insert id (speed / n(o effect)) (yaw / c(urrent) / n(o effect)) (pitch / c(urrent) / n(o effect))");
                return true;
            }
            int int12 = Integer.parseInt(array[1]);
            final Location location3 = ((Player)commandSender).getLocation();
            final List<Location> safeWaypoints2 = this.getSafeWaypoints(commandSender);
            if (int12 < 0 || int12 >= safeWaypoints2.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Position must be a non-negative integer.");
                return true;
            }
            if (int12 > safeWaypoints2.size()) {
                int12 = safeWaypoints2.size();
            }
            if (safeWaypoints2.size() > 0 && !safeWaypoints2.get(0).getWorld().getName().equalsIgnoreCase(location3.getWorld().getName())) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot add waypoints to another world!");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            safeWaypoints2.add(int12, location3);
            this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints2);
            final List<Double> safeWaypointSpeeds2 = this.getSafeWaypointSpeeds(commandSender);
            final List<Double> safeWaypointYaw3 = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch3 = this.getSafeWaypointPitch(commandSender);
            double n4 = -1.0;
            if (array.length > 2) {
                try {
                    n4 = Double.parseDouble(array[2]);
                    n4 = Math.abs(n4);
                }
                catch (Exception ex19) {}
            }
            double double3 = -1.0;
            if (array.length > 3) {
                if (array[3].equalsIgnoreCase("c")) {
                    double3 = ((Player)commandSender).getLocation().getYaw();
                }
                else if (array[3].equalsIgnoreCase("n")) {
                    double3 = 444.0;
                }
                else {
                    try {
                        double3 = Double.parseDouble(array[3]);
                    }
                    catch (Exception ex20) {}
                }
            }
            double double4 = -1.0;
            if (array.length > 4) {
                if (array[4].equalsIgnoreCase("c")) {
                    double4 = ((Player)commandSender).getLocation().getPitch();
                }
                else if (array[4].equalsIgnoreCase("n")) {
                    double4 = 444.0;
                }
                else {
                    try {
                        double4 = Double.parseDouble(array[4]);
                    }
                    catch (Exception ex21) {}
                }
            }
            double3 = formatAngleYaw(double3);
            double4 = formatAnglePitch(double4);
            safeWaypointSpeeds2.add(int12, n4);
            safeWaypointYaw3.add(int12, double3);
            safeWaypointPitch3.add(int12, double4);
            UUID u = ((Player)commandSender).getUniqueId();
            this.waypoints_s.put(u, safeWaypointSpeeds2);
            this.waypoints_y.put(u, safeWaypointYaw3);
            this.waypoints_p.put(u, safeWaypointPitch3);
            if (this.waypoints_m.get(u) == null) {
                this.waypoints_m.put(u, new ArrayList<String>());
            }
            this.waypoints_m.get(u).add(int12, "");
            if (this.waypoints_c.get(u) == null) {
                this.waypoints_c.put(u, new ArrayList<List<String>>());
            }
            this.waypoints_c.get(u).add(int12, new ArrayList<String>());
            this.getSafeWaypointDelays(commandSender);
            this.getSafeWaypointOptions(commandSender);
            this.getSafeWaypointInstants(commandSender);
            this.waypoints_l.get(u).add(int12, 0);
            this.waypoints_d.get(u).add(int12, 0.0);
            this.waypoints_i.get(u).add(int12, false);
            if (n4 >= 0.0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted waypoint at #" + int12 + ", setting the speed to " + n4);
            }
            else {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Inserted waypoint at #" + int12);
            }
        }
        else if (s2.equalsIgnoreCase("edit")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " edit <id> <speed / n(o effect)> <yaw / c(urrent) / n(o effect)> <pitch / c(urrent) / n(o effect)>");
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " (D(o not change) as any parameter will not change it)");
                return true;
            }
            final int int13 = Integer.parseInt(array[1]);
            final List<Location> safeWaypoints3 = this.getSafeWaypoints(commandSender);
            if (int13 < 0 && int13 >= safeWaypoints3.size()) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            final List<Double> safeWaypointSpeeds3 = this.getSafeWaypointSpeeds(commandSender);
            final List<Double> safeWaypointYaw4 = this.getSafeWaypointYaw(commandSender);
            final List<Double> safeWaypointPitch4 = this.getSafeWaypointPitch(commandSender);
            double n5 = -1.0;
            if (!array[2].equalsIgnoreCase("D")) {
                try {
                    n5 = Double.parseDouble(array[2]);
                    n5 = Math.abs(n5);
                }
                catch (Exception ex22) {}
            }
            double double5 = -1.0;
            if (!array[3].equalsIgnoreCase("D")) {
                if (array[3].equalsIgnoreCase("c")) {
                    double5 = ((Player)commandSender).getLocation().getYaw();
                }
                else if (array[3].equalsIgnoreCase("n")) {
                    double5 = 444.0;
                }
                else {
                    try {
                        double5 = Double.parseDouble(array[3]);
                    }
                    catch (Exception ex23) {}
                }
            }
            double double6 = -1.0;
            if (!array[4].equalsIgnoreCase("D")) {
                if (array[4].equalsIgnoreCase("c")) {
                    double6 = ((Player)commandSender).getLocation().getPitch();
                }
                else if (array[4].equalsIgnoreCase("n")) {
                    double6 = 444.0;
                }
                else {
                    try {
                        double6 = Double.parseDouble(array[4]);
                    }
                    catch (Exception ex24) {}
                }
            }
            if (!array[2].equalsIgnoreCase("D")) {
                safeWaypointSpeeds3.set(int13, n5);
            }
            if (!array[3].equalsIgnoreCase("D")) {
                safeWaypointYaw4.set(int13, double5);
            }
            if (!array[4].equalsIgnoreCase("D")) {
                safeWaypointPitch4.set(int13, double6);
            }
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds3);
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw4);
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch4);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Edited the properties of waypoint #" + int13);
        }
        else if (s2.equalsIgnoreCase("speed")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                double doubleValue3 = 5.0;
                try {
                    doubleValue3 = this.speed.get(((Player)commandSender).getUniqueId());
                }
                catch (Exception ex25) {}
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "Starting speed is currently " + make_color('a') + "" + doubleValue3);
                return true;
            }
            double double7;
            try {
                double7 = Double.parseDouble(array[1]);
            }
            catch (Exception ex9) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "That is not a number!");
                return true;
            }
            final double abs = Math.abs(double7);
            this.speed.put(((Player)commandSender).getUniqueId(), abs);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "Starting speed set to " + make_color('a') + "" + abs);
        }
        else if (s2.equalsIgnoreCase("fclear")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " fclear player");
                return true;
            }
            final Player player = this.getServer().getPlayer(array[1]);
            if (player == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            this.clearCache(player);
            this.clearPathName((Player)commandSender);
            this.waypoints.put(player.getUniqueId(), new ArrayList<Location>());
            this.waypoints_s.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_y.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_p.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_m.put(player.getUniqueId(), new ArrayList<String>());
            this.waypoints_c.put(player.getUniqueId(), new ArrayList<List<String>>());
            this.getSafeWaypointFlags(player);
            this.waypoints_f.put(player.getUniqueId(), 0);
            this.waypoints_t.put(player.getUniqueId(), -1.0);
            this.waypoints_d.put(player.getUniqueId(), new ArrayList<Double>());
            this.waypoints_l.put(player.getUniqueId(), new ArrayList<Integer>());
            this.waypoints_i.put(player.getUniqueId(), new ArrayList<Boolean>());
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path cleared.");
        }
        else if (s2.equalsIgnoreCase("fstop")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " fstop player");
                return true;
            }
            if (globalMode != null && array[1].equalsIgnoreCase("**")) {
                stopGlobal();
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "OK");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this. If you want to stop global playback: /" + s + " fstop **");
                return true;
            }
            final Player player = this.getServer().getPlayer(array[1]);
            if (player == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Stopped.");
            this.stop(player, PathPlaybackStoppedEvent.StopCause.FSTOP);
        }
        else if (s2.equalsIgnoreCase("fload") || s2.equalsIgnoreCase("fplay")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " " + s2.toLowerCase() + " player path (tp|notp|pathless)");
                return true;
            }
            if (s2.equalsIgnoreCase("fplay") && array[1].equalsIgnoreCase("**")) {
                if (getServer().getOnlinePlayers().size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "No players are online!");
                    return true;
                }
                Player up = commandSender instanceof Player ? ((Player)commandSender) : getServer().getOnlinePlayers().iterator().next();
                UUID u = up.getUniqueId();
                if (!pl_paths.containsKey(u)) {
                    pl_paths.put(u, new ArrayList<String>());
                }
                if (pl_paths.get(u).size() < 1) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your playlist is empty.");
                    return true;
                }
                pl_index.put(u, -1);
                if (!this.findNextSuitablePath(up)) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "The playlist has no playable paths!");
                    return true;
                }
                globalMode = u;
                pl_playing.put(u, true);
                pl_looping.put(u, true);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
                this.play(up, commandSender, false, PathPlaybackStartedEvent.StartCause.FPLAY);
                return true;
            }
            final Player player2 = this.getServer().getPlayer(array[1]);
            if (player2 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.isTrue(this.playing.get(player2.getUniqueId()))) {
                this.stop(player2, PathPlaybackStoppedEvent.StopCause.FSTOP);
            }
            if (array.length > 3) {
                if (array[3].equalsIgnoreCase("tp")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[3].equalsIgnoreCase("notp")) {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[3].equalsIgnoreCase("pathless")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), true);
                }
                else {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
            }
            try {
                final File file = new File(this.paths, array[2]);
                if (!file.isFile()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found");
                    return true;
                }
                final String s3 = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                this.clearCache(player2);
                UUID u2 = player2.getUniqueId();
                this.waypoints.put(u2, new ArrayList<Location>());
                this.waypoints_s.put(u2, new ArrayList<Double>());
                this.waypoints_y.put(u2, new ArrayList<Double>());
                this.waypoints_p.put(u2, new ArrayList<Double>());
                this.waypoints_m.put(u2, new ArrayList<String>());
                this.waypoints_c.put(u2, new ArrayList<List<String>>());
                this.waypoints_f.put(u2, 0);
                this.waypoints_t.put(u2, -1.0);
                this.waypoints_d.put(u2, new ArrayList<Double>());
                this.waypoints_l.put(u2, new ArrayList<Integer>());
                this.waypoints_i.put(u2, new ArrayList<Boolean>());
                final List<Location> safeWaypoints4 = this.getSafeWaypoints(player2);
                final List<Double> safeWaypointSpeeds4 = this.getSafeWaypointSpeeds(player2);
                final List<Double> safeWaypointYaw5 = this.getSafeWaypointYaw(player2);
                final List<Double> safeWaypointPitch5 = this.getSafeWaypointPitch(player2);
                final List<String> safeWaypointMessages2 = this.getSafeWaypointMessages(player2);
                final List<List<String>> safeWaypointCommands2 = this.getSafeWaypointCommands(player2);
                final World world = player2.getWorld();
                final String s4 = s3.split("#")[0];
                final String s5 = s3.split("#")[1];
                final float float1 = Float.parseFloat(s4.split(",")[2]);
                final float float2 = Float.parseFloat(s4.split(",")[3]);
                this.pathnames.put(u2, array[2]);
                int safeFlags = 0;
                if (s4.split(",").length > 4) {
                    safeFlags = Integer.parseInt(s4.split(",")[4]);
                }
                double safeTime = -1;
                if (s4.split(",").length > 5) {
                    safeTime = Double.parseDouble(s4.split(",")[5]);
                }
                int n6 = 0;
                String[] split;
                for (int length = (split = s5.split(Pattern.quote("|"))).length, l = 0; l < length; ++l) {
                    final String s6 = split[l];
                    try {
                        final String[] split2 = s6.split(",");
                        final Location location4 = new Location(world, Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2].split(";")[0]));
                        if (n6 == 0) {
                            location4.setYaw(float1);
                            location4.setPitch(float2);
                        }
                        safeWaypoints4.add(location4);
                        safeWaypointCommands2.add(new ArrayList<String>());
                        if (split2[3].indexOf(10) >= 0) {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3].split("\n")[0]));
                            int n7 = 0;
                            String[] split3;
                            for (int length2 = (split3 = split2[3].split("\n")).length, n8 = 0; n8 < length2; ++n8) {
                                final String s7 = split3[n8];
                                if (n7++ >= 1) {
                                    safeWaypointCommands2.get(n6).add(s7.replace("\uf555", ","));
                                }
                            }
                        }
                        else {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3]));
                        }
                        try {
                            if (split2[2].split(";").length > 3) {
                                this.waypoints_i.get(u2).add(!split2[2].split(";")[3].equalsIgnoreCase("0"));
                            } else {
                                this.waypoints_i.get(u2).add(false);
                            }
                            if (split2[2].split(";").length < 2) {
                                throw new ArrayIndexOutOfBoundsException();
                            }
                            double d = Double.parseDouble(split2[2].split(";")[1]);
                            int lf = Integer.parseInt(split2[2].split(";")[2]);
                            this.waypoints_d.get(u2).add(d);
                            this.waypoints_l.get(u2).add(lf);
                        } catch (Exception ex) {
                            this.waypoints_d.get(u2).add(0.0);
                            this.waypoints_l.get(u2).add(0);
                        }
                        if (split2.length > 4) {
                            final String[] split4 = split2[4].split(":");
                            final String[] split5 = split4[1].split("\\$", 2);
                            if (split5.length > 1) {
                                safeWaypointMessages2.add(split5[1].replace("\uf555", ","));
                            }
                            else {
                                safeWaypointMessages2.add("");
                            }
                            safeWaypointYaw5.add(this.formatAngleYaw(Double.parseDouble(split4[0])));
                            safeWaypointPitch5.add(this.formatAnglePitch(Double.parseDouble(split5[0])));
                        }
                        else {
                            safeWaypointYaw5.add(444.0);
                            safeWaypointPitch5.add(444.0);
                        }
                        ++n6;
                    }
                    catch (Exception ex10) {
                        if (safeWaypointYaw5.size() > safeWaypointPitch5.size()) {
                            safeWaypointYaw5.remove(safeWaypointYaw5.size() - 1);
                        }
                        if (safeWaypointMessages2.size() > safeWaypointYaw5.size()) {
                            safeWaypointMessages2.remove(safeWaypointMessages2.size() - 1);
                        }
                        if (safeWaypointSpeeds4.size() > safeWaypointYaw5.size()) {
                            safeWaypointSpeeds4.remove(safeWaypointSpeeds4.size() - 1);
                        }
                        if (safeWaypoints4.size() > safeWaypointSpeeds4.size()) {
                            safeWaypoints4.remove(safeWaypoints4.size() - 1);
                        }
                        if (safeWaypointCommands2.size() > safeWaypoints4.size()) {
                            safeWaypointCommands2.remove(safeWaypointCommands2.size() - 1);
                        }
                    }
                }
                this.waypoints_y.put(player2.getUniqueId(), safeWaypointYaw5);
                this.waypoints_p.put(player2.getUniqueId(), safeWaypointPitch5);
                this.waypoints_m.put(player2.getUniqueId(), safeWaypointMessages2);
                this.waypoints_c.put(player2.getUniqueId(), safeWaypointCommands2);
                this.waypoints_f.put(player2.getUniqueId(), safeFlags);
                this.waypoints_t.put(player2.getUniqueId(), safeTime);
                this.speed.put(player2.getUniqueId(), Double.parseDouble(s4.split(",")[1]));
                if (!world.getName().equalsIgnoreCase(s4.split(",")[0])) {
                    final World world2 = this.getServer().getWorld(s4.split(",")[0]);
                    if (world2 != null) {
                        final Iterator<Location> iterator3 = safeWaypoints4.iterator();
                        while (iterator3.hasNext()) {
                            iterator3.next().setWorld(world2);
                        }
                        player2.teleport(world2.getSpawnLocation());
                    }
                    else {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s4.split(",")[0] + "'.");
                    }
                }
                this.waypoints.put(player2.getUniqueId(), safeWaypoints4);
                this.waypoints_s.put(player2.getUniqueId(), safeWaypointSpeeds4);
            }
            catch (Exception ex11) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                return true;
            }
            //commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing!");
            pl_playing.put(player2.getUniqueId(), false);
            pl_looping.put(player2.getUniqueId(), false);
            if (s2.equalsIgnoreCase("fplay"))
                this.play(player2, commandSender, true, PathPlaybackStartedEvent.StartCause.FPLAY);
        }
        else if (s2.equalsIgnoreCase("ftplay")) {
            if (!commandSender.hasPermission("servercinematics.fplay")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " ftplay player path hours:minutes:seconds (tp|notp|pathless)");
                return true;
            }
            final Player player2 = this.getServer().getPlayer(array[1]);
            if (player2 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.isTrue(this.playing.get(player2.getUniqueId()))) {
                this.stop(player2, PathPlaybackStoppedEvent.StopCause.FSTOP);
            }
            if (array.length > 4) {
                if (array[4].equalsIgnoreCase("tp")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[4].equalsIgnoreCase("notp")) {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
                else if (array[4].equalsIgnoreCase("pathless")) {
                    this.teleport.put(player2.getUniqueId(), true);
                    this.pathless.put(player2.getUniqueId(), true);
                }
                else {
                    this.teleport.put(player2.getUniqueId(), false);
                    this.pathless.put(player2.getUniqueId(), false);
                }
            }

            try {
                final File file = new File(this.paths, array[2]);
                if (!file.isFile()) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path not found");
                    return true;
                }
                final String s3 = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                this.clearCache(player2);
                UUID u2 = player2.getUniqueId();
                this.waypoints.put(u2, new ArrayList<Location>());
                this.waypoints_s.put(u2, new ArrayList<Double>());
                this.waypoints_y.put(u2, new ArrayList<Double>());
                this.waypoints_p.put(u2, new ArrayList<Double>());
                this.waypoints_m.put(u2, new ArrayList<String>());
                this.waypoints_c.put(u2, new ArrayList<List<String>>());
                this.waypoints_f.put(u2, 0);
                this.waypoints_t.put(u2, -1.0);
                this.waypoints_d.put(u2, new ArrayList<Double>());
                this.waypoints_l.put(u2, new ArrayList<Integer>());
                this.waypoints_i.put(u2, new ArrayList<Boolean>());
                final List<Location> safeWaypoints4 = this.getSafeWaypoints(player2);
                final List<Double> safeWaypointSpeeds4 = this.getSafeWaypointSpeeds(player2);
                final List<Double> safeWaypointYaw5 = this.getSafeWaypointYaw(player2);
                final List<Double> safeWaypointPitch5 = this.getSafeWaypointPitch(player2);
                final List<String> safeWaypointMessages2 = this.getSafeWaypointMessages(player2);
                final List<List<String>> safeWaypointCommands2 = this.getSafeWaypointCommands(player2);
                final World world = player2.getWorld();
                final String s4 = s3.split("#")[0];
                final String s5 = s3.split("#")[1];
                final float float1 = Float.parseFloat(s4.split(",")[2]);
                final float float2 = Float.parseFloat(s4.split(",")[3]);
                this.pathnames.put(u2, array[2]);
                int safeFlags = 0;
                if (s4.split(",").length > 4) {
                    safeFlags = Integer.parseInt(s4.split(",")[4]);
                }
                double safeTime = -1;
                if (s4.split(",").length > 5) {
                    safeTime = Double.parseDouble(s4.split(",")[5]);
                }
                int n6 = 0;
                String[] split;
                for (int length = (split = s5.split(Pattern.quote("|"))).length, l = 0; l < length; ++l) {
                    final String s6 = split[l];
                    try {
                        final String[] split2 = s6.split(",");
                        final Location location4 = new Location(world, Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2].split(";")[0]));
                        if (n6 == 0) {
                            location4.setYaw(float1);
                            location4.setPitch(float2);
                        }
                        safeWaypoints4.add(location4);
                        safeWaypointCommands2.add(new ArrayList<String>());
                        if (split2[3].indexOf(10) >= 0) {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3].split("\n")[0]));
                            int n7 = 0;
                            String[] split3;
                            for (int length2 = (split3 = split2[3].split("\n")).length, n8 = 0; n8 < length2; ++n8) {
                                final String s7 = split3[n8];
                                if (n7++ >= 1) {
                                    safeWaypointCommands2.get(n6).add(s7.replace("\uf555", ","));
                                }
                            }
                        }
                        else {
                            safeWaypointSpeeds4.add(Double.parseDouble(split2[3]));
                        }
                        try {
                            if (split2[2].split(";").length > 3) {
                                this.waypoints_i.get(u2).add(!split2[2].split(";")[3].equalsIgnoreCase("0"));
                            } else {
                                this.waypoints_i.get(u2).add(false);
                            }
                            if (split2[2].split(";").length < 2) {
                                throw new ArrayIndexOutOfBoundsException();
                            }
                            double d = Double.parseDouble(split2[2].split(";")[1]);
                            int lf = Integer.parseInt(split2[2].split(";")[2]);
                            this.waypoints_d.get(u2).add(d);
                            this.waypoints_l.get(u2).add(lf);
                        } catch (Exception ex) {
                            this.waypoints_d.get(u2).add(0.0);
                            this.waypoints_l.get(u2).add(0);
                        }
                        if (split2.length > 4) {
                            final String[] split4 = split2[4].split(":");
                            final String[] split5 = split4[1].split("\\$", 2);
                            if (split5.length > 1) {
                                safeWaypointMessages2.add(split5[1].replace("\uf555", ","));
                            }
                            else {
                                safeWaypointMessages2.add("");
                            }
                            safeWaypointYaw5.add(this.formatAngleYaw(Double.parseDouble(split4[0])));
                            safeWaypointPitch5.add(this.formatAnglePitch(Double.parseDouble(split5[0])));
                        }
                        else {
                            safeWaypointYaw5.add(444.0);
                            safeWaypointPitch5.add(444.0);
                        }
                        ++n6;
                    }
                    catch (Exception ex10) {
                        if (safeWaypointYaw5.size() > safeWaypointPitch5.size()) {
                            safeWaypointYaw5.remove(safeWaypointYaw5.size() - 1);
                        }
                        if (safeWaypointMessages2.size() > safeWaypointYaw5.size()) {
                            safeWaypointMessages2.remove(safeWaypointMessages2.size() - 1);
                        }
                        if (safeWaypointSpeeds4.size() > safeWaypointYaw5.size()) {
                            safeWaypointSpeeds4.remove(safeWaypointSpeeds4.size() - 1);
                        }
                        if (safeWaypoints4.size() > safeWaypointSpeeds4.size()) {
                            safeWaypoints4.remove(safeWaypoints4.size() - 1);
                        }
                        if (safeWaypointCommands2.size() > safeWaypoints4.size()) {
                            safeWaypointCommands2.remove(safeWaypointCommands2.size() - 1);
                        }
                    }
                }
                this.waypoints_y.put(player2.getUniqueId(), safeWaypointYaw5);
                this.waypoints_p.put(player2.getUniqueId(), safeWaypointPitch5);
                this.waypoints_m.put(player2.getUniqueId(), safeWaypointMessages2);
                this.waypoints_c.put(player2.getUniqueId(), safeWaypointCommands2);
                this.waypoints_f.put(player2.getUniqueId(), safeFlags);
                this.waypoints_t.put(player2.getUniqueId(), safeTime);
                this.speed.put(player2.getUniqueId(), Double.parseDouble(s4.split(",")[1]));
                if (!world.getName().equalsIgnoreCase(s4.split(",")[0])) {
                    final World world2 = this.getServer().getWorld(s4.split(",")[0]);
                    if (world2 != null) {
                        final Iterator<Location> iterator3 = safeWaypoints4.iterator();
                        while (iterator3.hasNext()) {
                            iterator3.next().setWorld(world2);
                        }
                        player2.teleport(world2.getSpawnLocation());
                    }
                    else {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s4.split(",")[0] + "'.");
                    }
                }
                this.waypoints.put(player2.getUniqueId(), safeWaypoints4);
                this.waypoints_s.put(player2.getUniqueId(), safeWaypointSpeeds4);
            }
            catch (Exception ex11) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                return true;
            }
            //commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing!");
            String time = array[3];
            if (!waypoints_t.containsKey(player2.getUniqueId())) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            } else if (waypoints_t.get(player2.getUniqueId()) < 0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            }
            double sec = 0;
            try {
                String[] tok = time.split(":");
                if (tok.length > 3 || tok.length < 1) {
                    throw new IllegalArgumentException();
                }
                int x = 1;
                for (int i = tok.length - 1; i >= 0; i--) {
                    sec += (x == 1 ? Double.parseDouble(tok[i]) : Integer.parseInt(tok[i])) * x;
                    x *= 60;
                }
            } catch (Exception ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot parse the given time.");
                return true;
            }
            if (waypoints_t.get(player2.getUniqueId()) > 0) {
                // speed multiplier
                multipl.put(player2.getUniqueId(), waypoints_t.get(player2.getUniqueId()) / sec);
            }
            pl_playing.put(player2.getUniqueId(), false);
            pl_looping.put(player2.getUniqueId(), false);
            this.play(player2, commandSender, true, PathPlaybackStartedEvent.StartCause.FPLAY);
        }
        else if (s2.equalsIgnoreCase("goto")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " goto id");
                return true;
            }
            try {
                final List<Double> safeWaypointYaw6 = this.getSafeWaypointYaw(commandSender);
                final List<Double> safeWaypointPitch6 = this.getSafeWaypointPitch(commandSender);
                final int int14 = Integer.parseInt(array[1]);
                final Location location5 = this.getSafeWaypoints(commandSender).get(int14);
                if (!this.improper(safeWaypointYaw6.get(int14))) {
                    location5.setYaw((float)(double)safeWaypointYaw6.get(int14));
                }
                if (!this.improper(safeWaypointPitch6.get(int14))) {
                    location5.setPitch((float)(double)safeWaypointPitch6.get(int14));
                }
                ((Player)commandSender).teleport(location5);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Teleported.");
            }
            catch (NumberFormatException ex3) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex4) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
            }
            catch (Exception ex5) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not teleport!");
            }
        }
        else if (s2.equalsIgnoreCase("remove")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                final List<Location> safeWaypoints5 = this.getSafeWaypoints(commandSender);
                final int n9 = safeWaypoints5.size() - 1;
                if (n9 < 0) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Your path is already devoid of waypoints!");
                    return true;
                }
                safeWaypoints5.remove(n9);
                final List<Double> safeWaypointSpeeds5 = this.getSafeWaypointSpeeds(commandSender);
                safeWaypointSpeeds5.remove(n9);
                final List<Double> safeWaypointYaw7 = this.getSafeWaypointYaw(commandSender);
                safeWaypointYaw7.remove(n9);
                final List<Double> safeWaypointPitch7 = this.getSafeWaypointPitch(commandSender);
                safeWaypointPitch7.remove(n9);
                this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints5);
                this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds5);
                this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw7);
                this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch7);
                if (this.waypoints_m.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
                }
                this.waypoints_m.get(((Player)commandSender).getUniqueId()).remove(n9);
                if (this.waypoints_c.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
                }
                this.waypoints_c.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_d.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_l.get(((Player)commandSender).getUniqueId()).remove(n9);
                this.waypoints_i.get(((Player)commandSender).getUniqueId()).remove(n9);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed last point!");
            }
            try {
                final int int15 = Integer.parseInt(array[1]);
                final List<Location> safeWaypoints6 = this.getSafeWaypoints(commandSender);
                safeWaypoints6.remove(int15);
                this.clearCache((Player)commandSender);
                this.clearPathName((Player)commandSender);
                final List<Double> safeWaypointSpeeds6 = this.getSafeWaypointSpeeds(commandSender);
                safeWaypointSpeeds6.remove(int15);
                final List<Double> safeWaypointYaw8 = this.getSafeWaypointYaw(commandSender);
                safeWaypointYaw8.remove(int15);
                final List<Double> safeWaypointPitch8 = this.getSafeWaypointPitch(commandSender);
                safeWaypointPitch8.remove(int15);
                this.waypoints.put(((Player)commandSender).getUniqueId(), safeWaypoints6);
                this.waypoints_s.put(((Player)commandSender).getUniqueId(), safeWaypointSpeeds6);
                this.waypoints_y.put(((Player)commandSender).getUniqueId(), safeWaypointYaw8);
                this.waypoints_p.put(((Player)commandSender).getUniqueId(), safeWaypointPitch8);
                if (this.waypoints_m.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
                }
                this.waypoints_m.get(((Player)commandSender).getUniqueId()).remove(int15);
                if (this.waypoints_c.get(((Player)commandSender).getUniqueId()) == null) {
                    this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
                }
                this.waypoints_c.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_d.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_l.get(((Player)commandSender).getUniqueId()).remove(int15);
                this.waypoints_i.get(((Player)commandSender).getUniqueId()).remove(int15);
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Removed point #" + int15 + "!");
            }
            catch (NumberFormatException ex12) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Please type an integer.");
            }
            catch (IndexOutOfBoundsException ex13) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Waypoint not found.");
            }
            catch (Exception ex14) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not remove!");
            }
        }
        else if (s2.equalsIgnoreCase("clone")) {

            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " clone player");
                return true;
            }
            final Player player3 = this.getServer().getPlayer(array[1]);
            if (player3 == null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot find player!");
                return true;
            }
            if (this.speed.containsKey(player3.getUniqueId())) {
                this.speed.put(((Player)commandSender).getUniqueId(), this.speed.get(player3.getUniqueId()));
            }
            else {
                this.speed.remove(((Player)commandSender).getUniqueId());
            }
            this.clearCache((Player)commandSender);
            this.pathnames.put(((Player)commandSender).getUniqueId(), this.pathnames.get((((Player)commandSender).getUniqueId())));
            if (this.pathnames.get(((Player)commandSender).getUniqueId()) != null)
                this.clearPathName((Player)commandSender);
            this.waypoints.put(((Player)commandSender).getUniqueId(), this.getSafeWaypoints(player3));
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointSpeeds(player3));
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointYaw(player3));
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointPitch(player3));
            this.waypoints_m.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointMessages(player3));
            this.waypoints_c.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointCommands(player3));
            this.waypoints_d.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointDelays(player3));
            this.waypoints_l.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointOptions(player3));
            this.waypoints_i.put(((Player)commandSender).getUniqueId(), this.getSafeWaypointInstants(player3));
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Path cloned.");
        }
        else if (s2.equalsIgnoreCase("load")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                final StringBuilder sb4 = new StringBuilder();
                File[] listFiles;
                for (int length3 = (listFiles = this.paths.listFiles()).length, n10 = 0; n10 < length3; ++n10) {
                    final File file2 = listFiles[n10];
                    if (file2.isFile()) {
                        sb4.append(", ");
                        sb4.append(file2.getName());
                    }
                }
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "List of saved paths:");
                if (sb4.length() < 1) {
                    commandSender.sendMessage("" + make_color('c') + "No paths were found");
                    return true;
                }
                commandSender.sendMessage(sb4.toString().substring(2));
                return true;
            }
            else {
                try {
                    final File file3 = new File(this.paths, array[1]);
                    if (!file3.isFile()) {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Not found");
                        return true;
                    }
                    final String s8 = new String(Files.readAllBytes(file3.toPath()), StandardCharsets.UTF_8);
                    final Player player4 = (Player)commandSender;
                    final UUID u4 = player4.getUniqueId();
                    int n11 = 0;
                    this.clearCache((Player)commandSender);
                    this.waypoints.put(player4.getUniqueId(), new ArrayList<Location>());
                    this.waypoints_s.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_y.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_p.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_m.put(player4.getUniqueId(), new ArrayList<String>());
                    this.waypoints_c.put(player4.getUniqueId(), new ArrayList<List<String>>());
                    this.waypoints_f.put(player4.getUniqueId(), 0);
                    this.waypoints_t.put(player4.getUniqueId(), -1.0);
                    this.waypoints_d.put(player4.getUniqueId(), new ArrayList<Double>());
                    this.waypoints_l.put(player4.getUniqueId(), new ArrayList<Integer>());
                    this.waypoints_i.put(player4.getUniqueId(), new ArrayList<Boolean>());
                    final List<Location> safeWaypoints7 = this.getSafeWaypoints(player4);
                    final List<Double> safeWaypointSpeeds7 = this.getSafeWaypointSpeeds(player4);
                    final List<Double> safeWaypointYaw9 = this.getSafeWaypointYaw(player4);
                    final List<Double> safeWaypointPitch9 = this.getSafeWaypointPitch(player4);
                    final List<String> safeWaypointMessages3 = this.getSafeWaypointMessages(player4);
                    final List<List<String>> safeWaypointCommands3 = this.getSafeWaypointCommands(player4);
                    final World world3 = player4.getWorld();
                    final String s9 = s8.split("#")[0];
                    final String s10 = s8.split("#")[1];
                    final float float3 = Float.parseFloat(s9.split(",")[2]);
                    final float float4 = Float.parseFloat(s9.split(",")[3]);
                    this.pathnames.put(u4, array[1]);
                    int n12 = 0;
                    int safeFlags2 = 0;
                    if (s9.split(",").length > 4) {
                        safeFlags2 = Integer.parseInt(s9.split(",")[4]);
                    }
                    double safeTime = -1.0;
                    if (s9.split(",").length > 5) {
                        safeTime = Double.parseDouble(s9.split(",")[5]);
                    }
                    String[] split6;
                    for (int length4 = (split6 = s10.split(Pattern.quote("|"))).length, n13 = 0; n13 < length4; ++n13) {
                        final String s11 = split6[n13];
                        try {
                            final String[] split7 = s11.split(",");
                            final Location location6 = new Location(world3, Double.parseDouble(split7[0]), Double.parseDouble(split7[1]), Double.parseDouble(split7[2].split(";")[0]));
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
                                    this.waypoints_i.get(u4).add(!split7[2].split(";")[3].equalsIgnoreCase("0"));
                                } else {
                                    this.waypoints_i.get(u4).add(false);
                                }
                                if (split7[2].split(";").length < 2) {
                                    throw new ArrayIndexOutOfBoundsException();
                                }
                                double d = Double.parseDouble(split7[2].split(";")[1]);
                                int lf = Integer.parseInt(split7[2].split(";")[2]);
                                this.waypoints_d.get(u4).add(d);
                                this.waypoints_l.get(u4).add(lf);
                            } catch (Exception ex) {
                                this.waypoints_d.get(u4).add(0.0);
                                this.waypoints_l.get(u4).add(0);
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
                    this.waypoints.put(player4.getUniqueId(), safeWaypoints7);
                    this.waypoints_s.put(player4.getUniqueId(), safeWaypointSpeeds7);
                    this.waypoints_y.put(player4.getUniqueId(), safeWaypointYaw9);
                    this.waypoints_p.put(player4.getUniqueId(), safeWaypointPitch9);
                    this.waypoints_m.put(player4.getUniqueId(), safeWaypointMessages3);
                    this.waypoints_c.put(player4.getUniqueId(), safeWaypointCommands3);
                    this.waypoints_f.put(player4.getUniqueId(), safeFlags2);
                    this.waypoints_t.put(player4.getUniqueId(), safeTime);
                    this.speed.put(player4.getUniqueId(), Double.parseDouble(s9.split(",")[1]));
                    if (!world3.getName().equalsIgnoreCase(s9.split(",")[0])) {
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Warning: World name does not match with saved name! Proceed with caution.");
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "World name of saved path was '" + s9.split(",")[0] + "'.");
                    }
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Successfully loaded!");
                    if (n11 > 0)
                        commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Skipped " + n11 + " malformed entries");
                }
                catch (Exception ex) {
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Malformed file, loading failed / was not finished");
                    commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('f') + "" + ex.toString());
                }
            }
        }
        else if (s2.equalsIgnoreCase("save")) {
            if (!commandSender.hasPermission("servercinematics.edit")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " save filename");
                return true;
            }
            final String s13 = array[1];
            if (s13.contains(".")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Could not save");
                return true;
            }
            final StringBuilder sb5 = new StringBuilder();
            boolean b2 = true;
            final Player player5 = (Player)commandSender;
            final List<Location> safeWaypoints8 = this.getSafeWaypoints(player5);
            final List<Double> safeWaypointSpeeds8 = this.getSafeWaypointSpeeds(player5);
            final List<Double> safeWaypointYaw10 = this.getSafeWaypointYaw(player5);
            final List<Double> safeWaypointPitch10 = this.getSafeWaypointPitch(player5);
            final List<String> safeWaypointMessages4 = this.getSafeWaypointMessages(player5);
            final List<List<String>> safeWaypointCommands4 = this.getSafeWaypointCommands(player5);
            sb5.append(player5.getWorld().getName());
            sb5.append(",");
            Double value = this.speed.get(player5.getUniqueId());
            if (value == null) {
                value = 5.0;
            }
            sb5.append(value);
            sb5.append(",");
            float yaw = 0.0f;
            float pitch = 0.0f;
            if (safeWaypoints8.size() > 0) {
                yaw = safeWaypoints8.get(0).getYaw();
                pitch = safeWaypoints8.get(0).getPitch();
            }
            sb5.append(yaw);
            sb5.append(",");
            sb5.append(pitch);
            sb5.append(",");
            sb5.append(this.getSafeWaypointFlags(player5));
            sb5.append(",");
            UUID u4 = player5.getUniqueId();
            sb5.append(waypoints_t.containsKey(u4) ? waypoints_t.get(u4) : -1.0);
            sb5.append("#");
            for (int n16 = 0; n16 < safeWaypoints8.size(); ++n16) {
                if (b2) {
                    b2 = !b2;
                }
                else {
                    sb5.append("|");
                }
                final Location location7 = safeWaypoints8.get(n16);
                sb5.append(location7.getX());
                sb5.append(",");
                sb5.append(location7.getY());
                sb5.append(",");
                sb5.append(location7.getZ());
                sb5.append(";");
                sb5.append(waypoints_d.get(u4).get(n16));
                sb5.append(";");
                sb5.append(waypoints_l.get(u4).get(n16));
                sb5.append(";");
                sb5.append(waypoints_i.get(u4).get(n16) ? "1" : "0");
                sb5.append(",");
                sb5.append(safeWaypointSpeeds8.get(n16));
                if (safeWaypointCommands4.get(n16).size() > 0) {
                    for (final String s14 : safeWaypointCommands4.get(n16)) {
                        sb5.append("\n");
                        sb5.append(s14.replace(",", "\uf555"));
                    }
                }
                sb5.append(",");
                sb5.append(safeWaypointYaw10.get(n16) + ":" + safeWaypointPitch10.get(n16));
                sb5.append("$" + safeWaypointMessages4.get(n16).replace(",", "\uf555"));
            }
            try {
                final PrintWriter printWriter = new PrintWriter(new File(this.paths, s13), "UTF-8");
                printWriter.print(sb5.toString());
                printWriter.close();
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Saved: open with /" + s + " load " + s13);
                this.pathnames.put(u4, s13);
            }
            catch (Exception ex2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "" + ex2.toString());
            }
        }
        else if (s2.equalsIgnoreCase("clear")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            this.clearCache((Player)commandSender);
            this.clearPathName((Player)commandSender);
            this.waypoints.put(((Player)commandSender).getUniqueId(), new ArrayList<Location>());
            this.waypoints_s.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_y.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_p.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_m.put(((Player)commandSender).getUniqueId(), new ArrayList<String>());
            this.waypoints_c.put(((Player)commandSender).getUniqueId(), new ArrayList<List<String>>());
            this.getSafeWaypointFlags((Player)commandSender);
            this.waypoints_f.put(((Player)commandSender).getUniqueId(), 0);
            this.waypoints_t.put(((Player)commandSender).getUniqueId(), -1.0);
            this.waypoints_d.put(((Player)commandSender).getUniqueId(), new ArrayList<Double>());
            this.waypoints_l.put(((Player)commandSender).getUniqueId(), new ArrayList<Integer>());
            this.waypoints_i.put(((Player)commandSender).getUniqueId(), new ArrayList<Boolean>());
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Path cleared.");
        }
        else if (s2.equalsIgnoreCase("resume")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final UUID uniqueId2 = ((Player)commandSender).getUniqueId();
            if (!this.playing.containsKey(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.playing.get(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.paused.containsKey(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not paused!");
                return true;
            }
            if (!this.paused.get(uniqueId2)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not paused!");
                return true;
            }
            this.paused.put(uniqueId2, false);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Resuming...");
        }
        else if (s2.equalsIgnoreCase("pause")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            final Player player6 = (Player)commandSender;
            final UUID uniqueId3 = player6.getUniqueId();
            if (!this.playing.containsKey(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (!this.playing.get(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're not playing!");
                return true;
            }
            if (this.paused.containsKey(uniqueId3) && this.paused.get(uniqueId3)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You're already paused!");
                return true;
            }
            player6.setVelocity(new Vector(0.0, 0.0, 0.0));
            this.paused.put(uniqueId3, true);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('e') + "Paused: continue with /" + s + " resume");
        }
        else if (s2.equalsIgnoreCase("play")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Only players can run this.");
                return true;
            }
            if (this.isTrue(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Already playing!");
                return true;
            }
            final Player player7 = (Player)commandSender;
            pl_playing.put(player7.getUniqueId(), false);
            pl_looping.put(player7.getUniqueId(), false);
            player7.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
            this.play(player7, commandSender, false, PathPlaybackStartedEvent.StartCause.MANUAL);
        }
        else if (s2.equalsIgnoreCase("tplay")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (array.length < 2) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "/" + s + " tplay ((hours:)minutes:)seconds");
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Only players can run this.");
                return true;
            }
            if (this.isTrue(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Already playing!");
                return true;
            }
            String time = array[1];
            final Player player7 = (Player)commandSender;
            UUID u = player7.getUniqueId();
            if (!waypoints_t.containsKey(u)) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            } else if (waypoints_t.get(u) < 0) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "I don't know the playback time for this path yet, please play it once normally (and save if you want to cache it).");
                return true;
            }
            double sec = 0;
            try {
                String[] tok = time.split(":");
                if (tok.length > 3 || tok.length < 1) {
                    throw new IllegalArgumentException();
                }
                int x = 1;
                for (int i = tok.length - 1; i >= 0; i--) {
                    sec += (x == 1 ? Double.parseDouble(tok[i]) : Integer.parseInt(tok[i])) * x;
                    x *= 60;
                }
            } catch (Exception ex) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "Cannot parse the given time.");
                return true;
            }
            if (waypoints_t.get(u) > 0) {
                multipl.put(u, waypoints_t.get(u) / sec);
            }
            pl_playing.put(player7.getUniqueId(), false);
            pl_looping.put(player7.getUniqueId(), false);
            player7.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Playing...");
            this.play(player7, commandSender, false, PathPlaybackStartedEvent.StartCause.MANUAL);
        }
        else if (s2.equalsIgnoreCase("stop")) {
            if (!commandSender.hasPermission("servercinematics.play")) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this.");
                return true;
            }
            if (globalMode != null) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You can't use this." + (commandSender.hasPermission("servercinematics.edit") ? " If you want to stop global playback: /" + s + " fstop **" : ""));
                return true;
            }
            if (this.isFalse(this.playing.get(((Player)commandSender).getUniqueId()))) {
                commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('c') + "You are not playing!");
                return true;
            }
            this.stop((Player)commandSender, PathPlaybackStoppedEvent.StopCause.MANUAL);
            commandSender.sendMessage((shortPrefix ? "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('7') + "]" : "" + make_color('7') + "[" + make_color('8') + "" + make_color('l') + "[**]<| " + make_color('e') + "Server" + make_color('6') + "Cinematics" + make_color('7') + "]") + " " + make_color('a') + "Stopped.");
        }
        else {
            this.sendMultilineMessage(commandSender, helpString.replace("###", s), "" + make_color('7') + "");
        }
        return true;
    }

     */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("cam") || cmd.getName().equals("camera")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();

                if (sender instanceof Player) {
                    suggestions.add("help");
                    if (sender.hasPermission("servercinematics.play")) {
                        suggestions.add("tpmode");
                        suggestions.add("pathless");
                        suggestions.add("playlist");
                        suggestions.add("resume");
                        suggestions.add("pause");
                        suggestions.add("play");
                        suggestions.add("tplay");
                        suggestions.add("stop");
                    }
                    if (sender.hasPermission("servercinematics.edit")) {
                        suggestions.add("list");
                        suggestions.add("delay");
                        suggestions.add("option");
                        suggestions.add("flags");
                        suggestions.add("flag");
                        suggestions.add("reload");
                        suggestions.add("add");
                        suggestions.add("cmd");
                        suggestions.add("msg");
                        suggestions.add("insert");
                        suggestions.add("edit");
                        suggestions.add("speed");
                        suggestions.add("goto");
                        suggestions.add("remove");
                        suggestions.add("clone");
                        suggestions.add("load");
                        suggestions.add("save");
                        suggestions.add("clear");
                    }
                    if (sender.hasPermission("servercinematics.fplay")) {
                        suggestions.add("fclear");
                        suggestions.add("fload");
                        suggestions.add("fplay");
                        suggestions.add("fstop");
                        suggestions.add("ftplay");
                    }
                } else {
                    suggestions.add("fplay");
                    suggestions.add("fstop");
                    suggestions.add("fclear");
                    suggestions.add("fload");
                    suggestions.add("ftplay");
                }

                clipList(suggestions, args[0]);

                return suggestions;
            } else if (args.length == 2) {
                List<String> suggestions = new ArrayList<>();
                boolean returnEmpty = false;

                if (args[0].equalsIgnoreCase("delay")
                        || args[0].equalsIgnoreCase("option")
                        || args[0].equalsIgnoreCase("edit")
                        || args[0].equalsIgnoreCase("goto")
                        || args[0].equalsIgnoreCase("cmd")
                        || args[0].equalsIgnoreCase("msg")) {
                    returnEmpty = true;
                    int maxLen = plugin.getSafeWaypoints(sender).size();
                    for (int i = 0; i < maxLen; ++i) {
                        String n = Integer.toString(i);
                        if (n.startsWith(args[1])) {
                            suggestions.add(n);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("fclear")
                        || args[0].equalsIgnoreCase("fload")
                        || args[0].equalsIgnoreCase("fplay")
                        || args[0].equalsIgnoreCase("fstop")
                        || args[0].equalsIgnoreCase("ftplay")
                        || args[0].equalsIgnoreCase("clone")) {
                    returnEmpty = true;
                    for (Player p : getServer().getOnlinePlayers()) {
                        String n = p.getName();
                        if (n.toLowerCase().startsWith(args[1].toLowerCase())) {
                            suggestions.add(n);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("playlist")) {
                    suggestions.add("add");
                    suggestions.add("insert");
                    suggestions.add("remove");
                    suggestions.add("list");
                    suggestions.add("clear");
                    suggestions.add("play");
                    suggestions.add("loop");
                }

                clipList(suggestions, args[1]);

                if (returnEmpty || suggestions.size() > 0) {
                    return suggestions;
                } else {
                    return null;
                }
            } else if (args.length == 3) {
                List<String> suggestions = new ArrayList<>();

                if (args[0].equalsIgnoreCase("cmd")) {
                    suggestions.add("list");
                    suggestions.add("add");
                    suggestions.add("get");
                    suggestions.add("remove");
                } else if (args[0].equalsIgnoreCase("msg")) {
                    suggestions.add("set");
                    suggestions.add("setcolored");
                    suggestions.add("remove");
                }

                clipList(suggestions, args[2]);

                if (suggestions.size() > 0) {
                    return suggestions;
                } else {
                    return null;
                }
            }
            return null;
        }
        return null;
    }
}