package com.werewolf.game;

import com.werewolf.game.arena.Arena;
import com.werewolf.game.arena.ArenaManager;
import com.werewolf.game.commands.WerewolfCommand;
import com.werewolf.game.game.GameManager;
import com.werewolf.game.listeners.*;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class WerewolfPlugin extends JavaPlugin {

    private static WerewolfPlugin instance;

    private ArenaManager arenaManager;
    private GameManager gameManager;
    private MessageUtil messageUtil;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.gameManager = new GameManager(this);
        this.messageUtil = new MessageUtil(this);

        arenaManager.getWorldManager().getWorldsFolder();

        arenaManager.loadGameFromConfig();

        WerewolfCommand command = new WerewolfCommand(this);
        getCommand("werewolf").setExecutor(command);
        getCommand("werewolf").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodLevelListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerOffHandListener(this), this);

        getLogger().info(messageUtil.get("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (arenaManager != null && arenaManager.getGame() != null) {
            arenaManager.getGame().forceStop();
        }
        getLogger().info(messageUtil.get("plugin.disabled"));
    }

    public static WerewolfPlugin getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public String prefix() {
        return com.werewolf.game.util.ColorUtil.color(getConfig().getString("prefix", "&8[&cWerewolf&8] &r"));
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}
