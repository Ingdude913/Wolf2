package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
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

    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "Map Selector - Vote for a Map";

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();

    public static void open(WerewolfPlugin plugin, Arena arena, Player player) {
        List<String> worlds = plugin.getArenaManager().getAvailableWorlds();
        int size = ((worlds.size() / 9) + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(player, size, GUI_TITLE);
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
                    meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + world + " (Voted)");
                } else {
                    meta.setDisplayName(ChatColor.YELLOW + world);
                }
                List<String> lore = new ArrayList<>();
                int votes = voteCounts.getOrDefault(world, 0);
                lore.add(ChatColor.GRAY + "Votes: " + ChatColor.WHITE + votes);
                if (isVoted) {
                    lore.add(ChatColor.GREEN + "You voted for this map!");
                } else {
                    lore.add(ChatColor.YELLOW + "Click to vote for this map");
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
                meta.setDisplayName(ChatColor.RED + "No maps available");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Add world folders to");
                lore.add(ChatColor.GRAY + "plugins/Werewolf/World/");
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

    public static boolean isMapSelectorGUI(String title) {
        return title != null && ChatColor.stripColor(title).startsWith("Map Selector");
    }
}
