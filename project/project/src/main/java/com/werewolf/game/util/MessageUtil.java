package com.werewolf.game.util;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private final WerewolfPlugin plugin;
    private File messageFile;
    private FileConfiguration messageConfig;

    public MessageUtil(WerewolfPlugin plugin) {
        this.plugin = plugin;
        loadMessageFile();
    }

    private void loadMessageFile() {
        messageFile = new File(plugin.getDataFolder(), "message.yml");
        if (!messageFile.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    public void reload() {
        loadMessageFile();
    }

    public String get(String path, Map<String, String> placeholders) {
        String message = messageConfig.getString(path, "");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ColorUtil.color(message);
    }

    public String get(String path) {
        return get(path, null);
    }

    public String raw(String path, Map<String, String> placeholders) {
        String message = messageConfig.getString(path, "");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public String raw(String path) {
        return raw(path, null);
    }

    public String prefixed(String path, Map<String, String> placeholders) {
        return plugin.prefix() + get(path, placeholders);
    }

    public String prefixed(String path) {
        return prefixed(path, null);
    }

    public String getRoleName(String roleKey) {
        return get("roles." + roleKey + ".name");
    }

    public String getRoleDescription(String roleKey) {
        return get("roles." + roleKey + ".description");
    }

    public String getRoleNightStart(String roleKey) {
        return get("roles." + roleKey + ".night-start");
    }

    public String getRoleNightStart2(String roleKey) {
        return get("roles." + roleKey + ".night-start-2");
    }

    public String getRoleDayStart(String roleKey) {
        return get("roles." + roleKey + ".day-start");
    }

    public static Map<String, String> ph(Object... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1].toString());
        }
        return map;
    }
}
