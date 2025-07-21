package net.scape.project.chatLite.config;

import net.scape.project.chatLite.ChatLite;

public class ConfigUtils {

    public static String getGlobalFormat() {
        return ChatLite.get().getConfig().getString("chat.global-format");
    }

    public static String getGroupFormat(String rank) {
        if (!ChatLite.get().getConfig().getBoolean("chat.enable-group-format")) return getGlobalFormat();
        if (ChatLite.get().getConfig().getString("chat.groups." + rank) == null || !ChatLite.get().getConfig().isSet("chat.groups." + rank)) {
            if (ChatLite.get().getConfig().isSet("chat.groups.default")) {
                return ChatLite.get().getConfig().getString("chat.groups.default");
            } else {
                return getGlobalFormat();
            }
        }

        return ChatLite.get().getConfig().getString("chat.groups." + rank);
    }
}
