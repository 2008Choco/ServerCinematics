package optic_fusion1.servercinematics.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    private Utils() {

    }

    public static String colorize(String string) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        for (Matcher matcher = pattern.matcher(string); matcher.find(); matcher = pattern.matcher(string)) {
            String color = string.substring(matcher.start(), matcher.end());
            string = string.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
        }
        string = ChatColor.translateAlternateColorCodes('&', string);
        return string;
    }

    public static void sendMultilineMessage(CommandSender commandSender, String s, String s2) {
        String[] split;
        for (int length = (split = s.split("\\r?\\n")).length, i = 0; i < length; ++i) {
            commandSender.sendMessage(String.valueOf(String.valueOf(s2)) + split[i]);
        }
    }

    public static void clipList(List<String> list, String inp) {
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String sugg = iterator.next();
            if (!sugg.startsWith(inp)) {
                iterator.remove();
            }
        }
    }

}
