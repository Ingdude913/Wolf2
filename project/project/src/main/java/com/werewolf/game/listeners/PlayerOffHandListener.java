package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerOffHandListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerOffHandListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);

        // Only restrict off-hand item swapping for players inside an active arena
        if (arena == null) return;

        event.setCancelled(true);
    }
}