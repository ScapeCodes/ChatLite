package net.scape.project.chatLite;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.scape.project.chatLite.commands.ChatLiteCommand;
import net.scape.project.chatLite.config.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatLite extends JavaPlugin implements Listener {

    private static ChatLite chatLite;
    private Chat vaultChat;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Pattern to match legacy color codes (&a, &b, &1, etc)
    private final Pattern colorPattern = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    // Pattern to match hex colors like &#aabbcc
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public void onEnable() {
        chatLite = this;

        saveDefaultConfig();
        setupVault();

        getCommand("chatlite").setExecutor(new ChatLiteCommand(this));
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ChatLite enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatLite disabled.");
    }

    public static ChatLite get() {
        return chatLite;
    }

    public boolean isPAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private void setupVault() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            vaultChat = rsp.getProvider();
        }
    }

    // Supports both Folia & paper/spigot
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();

        String playerName = player.getName();
        String worldName = player.getWorld().getName();
        String displayName = player.getDisplayName();

        Runnable task = () -> {
            String rank = getRank(player);
            String format = ConfigUtils.getGroupFormat(rank);

            // Apply built-in placeholders
            format = format
                    .replace("%player%", playerName)
                    .replace("%displayname%", displayName)
                    .replace("%world%", worldName)
                    .replace("%rank%", rank)
                    .replace("%message%", rawMessage);

            // Vault placeholders
            if (vaultChat != null) {
                String prefix = vaultChat.getPlayerPrefix(player);
                String suffix = vaultChat.getPlayerSuffix(player);
                format = format.replace("%prefix%", prefix != null ? prefix : "")
                        .replace("%suffix%", suffix != null ? suffix : "");
            }

            // PlaceholderAPI
            if (isPAPIEnabled()) {
                format = PlaceholderAPI.setPlaceholders(player, format);
            }

            boolean useMiniMessage = getConfig().getBoolean("chat.use-minimessage", false);
            if (useMiniMessage) {
                format = legacyToMiniMessage(format, player);
                String msgFormatted = legacyToMiniMessage(rawMessage, player);
                format = format.replace("%message%", msgFormatted);
                Component chatComponent = miniMessage.deserialize(format);

                event.getRecipients().forEach(p -> p.sendMessage(chatComponent));
                event.setCancelled(true);
            } else {
                format = applyColorCodes(format, player);
                String msgFormatted = applyColorCodes(rawMessage, player);
                format = format.replace("%message%", msgFormatted);
                event.setFormat(format);
            }
        };

        try {
            Bukkit.getRegionScheduler().execute(this, player.getLocation(), task);
        } catch (NoClassDefFoundError e) {
            Bukkit.getScheduler().runTask(this, task);
        }
    }


//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onChat(AsyncPlayerChatEvent event) {
//        Player player = event.getPlayer();
//        String message = event.getMessage();
//        String rank = getRank(player);
//        String format = ConfigUtils.getGroupFormat(rank);
//
//        // Built-in placeholders
//        format = format
//                .replace("%player%", player.getName())
//                .replace("%displayname%", player.getDisplayName())
//                .replace("%world%", player.getWorld().getName())
//                .replace("%message%", message)
//                .replace("%rank%", rank);
//
//        // Vault prefix/suffix
//        if (vaultChat != null) {
//            String prefix = vaultChat.getPlayerPrefix(player);
//            String suffix = vaultChat.getPlayerSuffix(player);
//            format = format.replace("%prefix%", prefix == null ? "" : prefix)
//                    .replace("%suffix%", suffix == null ? "" : suffix);
//        }
//
//        // PlaceholderAPI
//        if (isPAPIEnabled()) {
//            format = PlaceholderAPI.setPlaceholders(player, format);
//        }
//
//        // Apply color codes and MiniMessage formatting based on config
//        boolean useMiniMessage = getConfig().getBoolean("chat.use-minimessage", false);
//
//        if (useMiniMessage) {
//            // Convert legacy codes and hex to MiniMessage tags in format and message
//            format = legacyToMiniMessage(format, player);
//            message = legacyToMiniMessage(message, player);
//
//            // Replace %message% placeholder with the parsed message
//            format = format.replace("%message%", message);
//
//            // Parse the full MiniMessage formatted chat string to Component
//            Component chatComponent = miniMessage.deserialize(format);
//
//            // Set formatted message using Adventure API (requires Paper or supported platform)
//            event.setFormat("%s"); // Dummy format, we override message directly
//            event.getRecipients().forEach(recipient -> {
//                if (recipient instanceof Player) {
//                    ((Player) recipient).sendMessage(chatComponent);
//                }
//            });
//            event.setCancelled(true); // Cancel default chat to prevent double message
//
//        } else {
//            // Old-style legacy color codes processing
//            format = applyColorCodes(format, player);
//            message = applyColorCodes(message, player);
//            format = format.replace("%message%", message);
//
//            event.setFormat(format);
//        }
//    }

    private String getRank(Player player) {
        if (vaultChat != null) {
            String group = vaultChat.getPrimaryGroup(player);
            return group == null ? "default" : group.toLowerCase();
        }
        return "default";
    }

    private String applyColorCodes(String input, Player player) {
        boolean colorPerm = getConfig().getBoolean("chat.require-permission-for-colors");
        String permPrefix = getConfig().getString("chat.color-permission-prefix", "chat.color.");
        boolean allowRgb = getConfig().getBoolean("chat.allow-rgb");

        if (colorPerm) {
            Matcher matcher = colorPattern.matcher(input);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String code = matcher.group(1).toLowerCase();
                if (player.hasPermission(permPrefix + code)) {
                    matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + code);
                } else {
                    matcher.appendReplacement(buffer, "&" + code); // retain if no permission
                }
            }
            matcher.appendTail(buffer);
            input = buffer.toString();
        } else {
            input = ChatColor.translateAlternateColorCodes('&', input);
        }

        if (allowRgb) {
            Matcher hex = hexPattern.matcher(input);
            while (hex.find()) {
                String color = hex.group(1);
                input = input.replace("&#" + color, net.md_5.bungee.api.ChatColor.of("#" + color).toString());
            }
        }

        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Converts legacy color codes (&a, &b, &1, etc) and hex codes (&#aabbcc) in the input string
     * into MiniMessage format tags like <green> or <#aabbcc>.
     * Honors color permissions when converting legacy codes.
     */
    private String legacyToMiniMessage(String input, Player player) {
        // Replace hex colors first
        Matcher hexMatcher = hexPattern.matcher(input);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String hexCode = hexMatcher.group(1);
            hexMatcher.appendReplacement(hexBuffer, "<#" + hexCode + ">");
        }
        hexMatcher.appendTail(hexBuffer);
        input = hexBuffer.toString();

        // Replace legacy color codes with MiniMessage tags
        Matcher matcher = colorPattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase();

            // Check permission for color codes only if config requires it
            boolean colorPerm = getConfig().getBoolean("chat.require-permission-for-colors");
            String permPrefix = getConfig().getString("chat.color-permission-prefix", "chat.color.");
            boolean allowed = !colorPerm || player.hasPermission(permPrefix + code);

            if (!allowed) {
                // Keep literal code if no permission
                matcher.appendReplacement(buffer, "&" + code);
                continue;
            }

            String mmTag = convertLegacyCodeToMiniMessageTag(code);
            matcher.appendReplacement(buffer, mmTag);
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Maps legacy color codes to MiniMessage color tags.
     */
    private String convertLegacyCodeToMiniMessageTag(String code) {
        return switch (code) {
            case "0" -> "<black>";
            case "1" -> "<dark_blue>";
            case "2" -> "<dark_green>";
            case "3" -> "<dark_aqua>";
            case "4" -> "<dark_red>";
            case "5" -> "<dark_purple>";
            case "6" -> "<gold>";
            case "7" -> "<gray>";
            case "8" -> "<dark_gray>";
            case "9" -> "<blue>";
            case "a" -> "<green>";
            case "b" -> "<aqua>";
            case "c" -> "<red>";
            case "d" -> "<light_purple>";
            case "e" -> "<yellow>";
            case "f" -> "<white>";
            case "k" -> "<obfuscated>";
            case "l" -> "<bold>";
            case "m" -> "<strikethrough>";
            case "n" -> "<underline>";
            case "o" -> "<italic>";
            case "r" -> "<reset>";
            default -> "";
        };
    }
}
