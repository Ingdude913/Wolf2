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
import java.util.*;

public class ArenaManager {

    private final WerewolfPlugin plugin;
    private Arena game;
    private final WorldManager worldManager;
    private File gameFile;
    private FileConfiguration gameConfig;
    private Location globalLobby;

    private final Map<String, Location> worldSpawns = new HashMap<>();

    public ArenaManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new WorldManager(plugin);
        loadGameFile();
        loadGlobalLobby();
        loadWorldSpawns();
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

    private void loadWorldSpawns() {
        ConfigurationSection section = gameConfig.getConfigurationSection("world-spawns");
        if (section == null) return;
        for (String worldName : section.getKeys(false)) {
            ConfigurationSection worldSection = section.getConfigurationSection(worldName);
            if (worldSection == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = worldManager.loadWorld(worldName);
            }
            if (world == null) continue;
            double x = worldSection.getDouble("x");
            double y = worldSection.getDouble("y");
            double z = worldSection.getDouble("z");
            float yaw = (float) worldSection.getDouble("yaw", 0);
            float pitch = (float) worldSection.getDouble("pitch", 0);
            worldSpawns.put(worldName, new Location(world, x, y, z, yaw, pitch));
            plugin.getLogger().info("Loaded spawn for world '" + worldName + "'.");
        }
    }

    public void setWorldSpawn(String worldName, Location loc) {
        if (loc == null) {
            worldSpawns.remove(worldName);
            gameConfig.set("world-spawns." + worldName, null);
        } else {
            worldSpawns.put(worldName, loc);
            gameConfig.set("world-spawns." + worldName + ".x", loc.getX());
            gameConfig.set("world-spawns." + worldName + ".y", loc.getY());
            gameConfig.set("world-spawns." + worldName + ".z", loc.getZ());
            gameConfig.set("world-spawns." + worldName + ".yaw", loc.getYaw());
            gameConfig.set("world-spawns." + worldName + ".pitch", loc.getPitch());
        }
        saveGameFile();
    }

    public Location getWorldSpawn(String worldName) {
        return worldSpawns.get(worldName);
    }

    public List<String> getAvailableWorlds() {
        File worldsFolder = worldManager.getWorldsFolder();
        File[] dirs = worldsFolder.listFiles(File::isDirectory);
        List<String> worlds = new ArrayList<>();
        if (dirs != null) {
            for (File dir : dirs) {
                worlds.add(dir.getName());
            }
        }
        Collections.sort(worlds);
        return worlds;
    }

    public void loadGameFromConfig() {
        String worldName = gameConfig.getString("active-game-world");
        if (worldName == null) return;
        worldManager.loadWorld(worldName);
        game = new Arena(plugin, "game", worldName);
        Location spawn = worldSpawns.get(worldName);
        if (spawn != null) {
            game.setSpawnLocation(spawn);
        }
        loadGameSettings(game);
        plugin.getLogger().info("Restored game arena in world '" + worldName + "'.");
    }

    public void saveGame() {
        saveGameFile();
    }

    public void saveGameSettings(Arena arena) {
        if (arena == null) return;
        gameConfig.set("settings.sheriff-enabled", arena.isSheriffEnabled());
        gameConfig.set("settings.selected-map", arena.getSelectedMapName());
        Map<String, Integer> roleSelection = arena.getRoleSelection();
        for (Map.Entry<String, Integer> entry : roleSelection.entrySet()) {
            gameConfig.set("settings.roles." + entry.getKey(), entry.getValue());
        }
        saveGameFile();
    }

    public void loadGameSettings(Arena arena) {
        if (arena == null) return;
        ConfigurationSection settings = gameConfig.getConfigurationSection("settings");
        if (settings == null) return;
        arena.setSheriffEnabled(settings.getBoolean("sheriff-enabled", true));
        String map = settings.getString("selected-map");
        if (map != null) {
            arena.setSelectedMap(map);
        }
        ConfigurationSection roles = settings.getConfigurationSection("roles");
        if (roles != null) {
            for (String roleKey : roles.getKeys(false)) {
                int count = roles.getInt(roleKey);
                if (count > 0) {
                    arena.getRoleSelection().put(roleKey, count);
                }
            }
        }
    }

    private void saveGameFile() {
        try {
            gameConfig.save(gameFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save game.yml: " + e.getMessage());
        }
    }

    public Arena createGame(String worldName) {
        if (game != null) {
            game.forceStop();
        }
        game = new Arena(plugin, "game", worldName);
        Location spawn = worldSpawns.get(worldName);
        if (spawn != null) {
            game.setSpawnLocation(spawn);
        }
        loadGameSettings(game);
        gameConfig.set("active-game-world", worldName);
        saveGameFile();
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
        gameConfig.set("active-game-world", null);
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
