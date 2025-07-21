# ChatLite

ChatLite is a lightweight, modern Minecraft chat formatter with built-in support for placeholders, color permissions, and rich formatting via MiniMessage. Designed to be clean, dependable, and compatible with modern servers including **Folia**, **Paper**, and **Spigot**.

---

## ✅ Features

- 🧩 Built-in placeholders like `%player%`, `%world%`, `%message%`, etc.
- 🎨 Color & format code permissions (`chat.color.a`, `chat.color.bold`, etc.)
- 🌈 Optional RGB/Hex color support (`&#aabbcc`)
- 💬 Group-based chat formatting with Vault
- 💡 PlaceholderAPI integration
- 📝 MiniMessage support (Adventure API)
- 🚀 Folia-compatible message dispatching
- 🔄 `/chatlite reload` command to reload config on the fly

---

## 🔧 Configuration

Example `config.yml`:

```yaml
# Built-in plugin placeholders you can use in chat formats:
# - %player%       : Player's username
# - %displayname%  : Player's display name
# - %world%        : Name of the world the player is currently in
# - %message%      : The chat message sent by the player
# - %rank%         : Player's rank/group name (via Vault)
# - %prefix%       : Player prefix from Vault (if available)
# - %suffix%       : Player suffix from Vault (if available)
#
# Supports PlaceholderAPI placeholders if the plugin is installed.

chat:
  # If true, players need permission to use color codes in chat messages.
  require-permission-for-colors: true

  # Prefix used for permission-based color codes (e.g., chat.color.a)
  color-permission-prefix: "chat.color."

  # Allow RGB hex color codes like '&#ff5733'
  allow-rgb: false

  # Use MiniMessage formatting (Adventure API)
  use-minimessage: false

  # Enable group-specific formatting based on Vault group
  enable-group-format: true

  # Default format used if no group format applies
  global-format: "&7[&bGlobal&7] &a%player%: &f%message%"

  # Formats by Vault group (fallback to 'default' if group not found)
  groups:
    default: "&7[&f%player%&7]: &f%message%"
    admin: "&c[Admin] &c%player%: &f%message%"
    mod: "&2[Mod] &a%player%: &f%message%"
    vip: "&6[VIP] &e%player%: &f%message%"
