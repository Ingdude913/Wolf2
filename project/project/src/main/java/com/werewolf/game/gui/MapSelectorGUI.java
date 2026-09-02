package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapSelectorGUI {

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();

    public static String getTitle(WerewolfPlugin plugin) {
        return plugin.getMessageUtil().get("gui.map-selector-title");
    }

    public static void open(WerewolfPlugin plugin, Arena arena, Player player) {
        MessageUtil msg = plugin.getMessageUtil();
        List<String> worlds = plugin.getArenaManager().getAvailableWorlds();
        int size = ((worlds.size() / 9) + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        String title = getTitle(plugin);
        Inventory inv = Bukkit.createInventory(player, size, title);
        Map<Integer, String> slotMap = new HashMap<>();

        Map<String, Integer> voteCounts = new HashMap<>();
        for (String world : arena.getMapVotes().values()) {
            voteCounts.merge(world, 1, Integer::sum);
        }

        String playerVote = arena.getPlayerMapVote(player);

        int slot = 0;
        for (String world : worlds) {
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                boolean isVoted = world.equals(playerVote);
                if (isVoted) {
                    meta.setDisplayName(msg.get("gui-items.map-world-voted", MessageUtil.ph("world", world)));
                } else {
                    meta.setDisplayName(msg.get("gui-items.map-world", MessageUtil.ph("world", world)));
                }
                List<String> lore = new ArrayList<>();
                int votes = voteCounts.getOrDefault(world, 0);
                lore.add(msg.get("gui-items.map-votes", MessageUtil.ph("votes", String.valueOf(votes))));
                if (isVoted) {
                    lore.add(msg.get("gui-items.map-voted-yes"));
                } else {
                    lore.add(msg.get("gui-items.map-voted-no"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slotMap.put(slot, world);
            slot++;
        }

        if (worlds.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(msg.get("gui-items.map-no-maps"));
                List<String> lore = new ArrayList<>();
                lore.add(msg.get("gui-items.map-no-maps-lore-1"));
                lore.add(msg.get("gui-items.map-no-maps-lore-2"));
                meta.setLore(lore);
                empty.setItemMeta(meta);
            }
            inv.setItem(4, empty);
        }

        guiMappings.put(player, slotMap);
        player.openInventory(inv);
    }

    public static String getWorldAtSlot(Player player, int slot) {
        Map<Integer, String> map = guiMappings.get(player);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player player) {
        guiMappings.remove(player);
    }

    public static boolean isMapSelectorGUI(WerewolfPlugin plugin, String title) {
        if (title == null) return false;
        String configTitle = ChatColor.stripColor(getTitle(plugin));
        return ChatColor.stripColor(title).startsWith(configTitle);
    }
}
