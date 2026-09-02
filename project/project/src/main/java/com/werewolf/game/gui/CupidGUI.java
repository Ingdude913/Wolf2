package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.roles.CupidRole;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

public class CupidGUI {

    private static final Map<Player, Map<Integer, Player>> guiMappings = new HashMap<>();

    public static String getTitle(WerewolfPlugin plugin) {
        return plugin.getMessageUtil().get("gui.cupid-title");
    }

    public static void open(WerewolfPlugin plugin, Arena arena, Player cupid) {
        String title = getTitle(plugin);
        Inventory inv = Bukkit.createInventory(cupid, 54, title);

        Map<Integer, Player> slotMap = new HashMap<>();
        int slot = 0;

        GamePlayer cupidGp = arena.getGamePlayer(cupid);
        CupidRole cupidRole = cupidGp != null ? cupidGp.asCupid() : null;
        Player selected1 = cupidRole != null ? cupidRole.getSpouse1() : null;
        Player selected2 = cupidRole != null ? cupidRole.getSpouse2() : null;

        for (GamePlayer gp : arena.getAlivePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(gp.getPlayer());
                boolean isSelected = (selected1 != null && gp.getPlayer().getUniqueId().equals(selected1.getUniqueId()))
                        || (selected2 != null && gp.getPlayer().getUniqueId().equals(selected2.getUniqueId()));
                if (isSelected) {
                    meta.setDisplayName(plugin.getMessageUtil().get("gui-items.cupid-player-selected", MessageUtil.ph("player", gp.getPlayer().getName())));
                } else {
                    meta.setDisplayName(plugin.getMessageUtil().get("gui-items.cupid-player", MessageUtil.ph("player", gp.getPlayer().getName())));
                }
                skull.setItemMeta(meta);
            }
            inv.setItem(slot, skull);
            slotMap.put(slot, gp.getPlayer());
            slot++;
        }

        guiMappings.put(cupid, slotMap);
        cupid.openInventory(inv);
    }

    public static Player getPlayerAtSlot(Player cupid, int slot) {
        Map<Integer, Player> map = guiMappings.get(cupid);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player cupid) {
        guiMappings.remove(cupid);
    }

    public static boolean isCupidGUI(WerewolfPlugin plugin, String title) {
        if (title == null) return false;
        String configTitle = ChatColor.stripColor(getTitle(plugin));
        return ChatColor.stripColor(title).startsWith(configTitle);
    }
}
