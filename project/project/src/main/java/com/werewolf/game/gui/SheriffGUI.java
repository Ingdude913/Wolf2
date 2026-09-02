package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

public class SheriffGUI {

    private static final Map<Player, Map<Integer, Player>> guiMappings = new HashMap<>();

    public static String getTitle(WerewolfPlugin plugin) {
        return plugin.getMessageUtil().get("gui.sheriff-title");
    }

    public static void open(WerewolfPlugin plugin, Arena arena, Player voter) {
        String title = getTitle(plugin);
        Inventory inv = Bukkit.createInventory(voter, 54, title);

        Map<Integer, Player> slotMap = new HashMap<>();
        int slot = 0;

        for (GamePlayer gp : arena.getAlivePlayers()) {
            if (gp.getPlayer().getUniqueId().equals(voter.getUniqueId())) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(gp.getPlayer());
                meta.setDisplayName(plugin.getMessageUtil().get("gui-items.sheriff-player", MessageUtil.ph("player", gp.getPlayer().getName())));
                skull.setItemMeta(meta);
            }
            inv.setItem(slot, skull);
            slotMap.put(slot, gp.getPlayer());
            slot++;
        }

        guiMappings.put(voter, slotMap);
        voter.openInventory(inv);
    }

    public static Player getPlayerAtSlot(Player voter, int slot) {
        Map<Integer, Player> map = guiMappings.get(voter);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player voter) {
        guiMappings.remove(voter);
    }

    public static boolean isSheriffGUI(WerewolfPlugin plugin, String title) {
        if (title == null) return false;
        String configTitle = ChatColor.stripColor(getTitle(plugin));
        return ChatColor.stripColor(title).startsWith(configTitle);
    }
}
