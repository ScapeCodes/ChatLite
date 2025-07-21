package net.scape.project.chatLite.commands;

import net.scape.project.chatLite.ChatLite;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatLiteCommand implements CommandExecutor {

    private final ChatLite plugin;

    public ChatLiteCommand(ChatLite plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatlite.reload")) {
                sender.sendMessage("§cYou do not have permission to reload ChatLite.");
                return true;
            }

            plugin.reloadConfig();
            sender.sendMessage("§aChatLite configuration reloaded.");
            return true;
        }

        sender.sendMessage("§cUsage: /chatlite reload");
        return true;
    }
}
