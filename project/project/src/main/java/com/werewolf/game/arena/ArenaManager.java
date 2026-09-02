package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.util.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class ArenaManager {

    private final WerewolfPlugin plugin;
    private Arena game;
    private final WorldManager worldManager;
    private File gameFile;
    private FileConfiguration gameConfig;
    private Location globalLobby;

    public ArenaManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new WorldManager(plugin);
        loadGameFile();
        loadGlobalLobby();
    }

    public Location getGlobalLobby() {
        return globalLobby;
    }

    public void setGlobalLobby(Location loc) {
        this.globalLobby = loc;
        if (loc != null) {
            gameConfig.set("global-lobby.world", loc.getWorld().getName());
            gameConfig.set("global-lobby.x", loc.getX());
            gameConfig.set("global-lobby.y", loc.getY());
            gameConfig.set("global-lobby.z", loc.getZ());
            gameConfig.set("global-lobby.yaw", loc.getYaw());
            gameConfig.set("global-lobby.pitch", loc.getPitch());
        } else {
            gameConfig.set("global-lobby", null);
        }
        saveGameFile();
    }

    private void loadGlobalLobby() {
        ConfigurationSection section = gameConfig.getConfigurationSection("global-lobby");
        if (section == null) return;
        String worldName = section.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = worldManager.loadWorld(worldName);
        }
        if (world == null) {
            plugin.getLogger().warning("Could not load world '" + worldName + "' for global lobby.");
            return;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        globalLobby = new Location(world, x, y, z, yaw, pitch);
        plugin.getLogger().info("Loaded global lobby in world '" + worldName + "'.");
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    private void loadGameFile() {
        gameFile = new File(plugin.getDataFolder(), "game.yml");
        if (!gameFile.exists()) {
            try {
                gameFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create game.yml: " + e.getMessage());
            }
        }
        gameConfig = YamlConfiguration.loadConfiguration(gameFile);
    }

    public void loadGameFromConfig() {
        if (gameConfig == null) return;

        ConfigurationSection section = gameConfig.getConfigurationSection("game");
        if (section == null) return;

        String worldName = section.getString("world");
        if (worldName == null) return;

        World world = worldManager.loadWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Could not load world '" + worldName + "' for the game. Skipping.");
            return;
        }

        game = new Arena(plugin, "game", worldName);

        Location spawn = deserializeLocation(section, "spawn", world);
        if (spawn != null) game.setSpawnLocation(spawn);

        plugin.getLogger().info("Loaded game (world: " + worldName + ").");
    }

    public void saveGame() {
        if (gameConfig == null || game == null) return;

        String path = "game";
        gameConfig.set(path + ".world", game.getWorldName());

        serializeLocation(game.getSpawnLocation(), path + ".spawn");

        saveGameFile();
    }

    private void saveGameFile() {
        try {
            gameConfig.save(gameFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save game.yml: " + e.getMessage());
        }
    }

    private void serializeLocation(Location loc, String path) {
        if (loc == null) {
            gameConfig.set(path, null);
            return;
        }
        gameConfig.set(path + ".x", loc.getX());
        gameConfig.set(path + ".y", loc.getY());
        gameConfig.set(path + ".z", loc.getZ());
        gameConfig.set(path + ".yaw", loc.getYaw());
        gameConfig.set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection parent, String key, World world) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) return null;
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Arena createGame(String worldName) {
        if (game != null) {
            game.forceStop();
        }
        game = new Arena(plugin, "game", worldName);
        saveGame();
        return game;
    }

    public Arena getGame() {
        return game;
    }

    public boolean gameExists() {
        return game != null;
    }

    public void deleteGame() {
        if (game != null) {
            game.forceStop();
            game = null;
        }
        gameConfig.set("game", null);
        saveGameFile();
    }

    public Collection<Arena> getArenas() {
        if (game == null) return Collections.emptyList();
        return Collections.singleton(game);
    }

    public Arena getArenaByPlayer(Player player) {
        if (game != null && game.isPlayerInArena(player)) {
            return game;
        }
        return null;
    }
}
