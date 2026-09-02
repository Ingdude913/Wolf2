package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.util.ColorUtil;
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

public class NinjaGUI {

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();

    public static final String ABILITY_VANISH = "vanish";
    public static final String ABILITY_SPRINT = "sprint";
    public static final String ABILITY_DECOY = "decoy";
    public static final String ABILITY_DISGUISE = "disguise";

    public static String getTitle(WerewolfPlugin plugin) {
        return plugin.getMessageUtil().get("gui.ninja-title");
    }

    public static void open(WerewolfPlugin plugin, Player ninja) {
        String title = getTitle(plugin);
        Inventory inv = Bukkit.createInventory(ninja, 27, title);

        Map<Integer, String> slotMap = new HashMap<>();

        ItemStack vanish = createAbilityItem(plugin, Material.ENDER_PEARL, "gui-items.ninja-vanish", "gui-items.ninja-vanish-lore");
        inv.setItem(11, vanish);
        slotMap.put(11, ABILITY_VANISH);

        ItemStack sprint = createAbilityItem(plugin, Material.SUGAR, "gui-items.ninja-sprint", "gui-items.ninja-sprint-lore");
        inv.setItem(12, sprint);
        slotMap.put(12, ABILITY_SPRINT);

        ItemStack decoy = createAbilityItem(plugin, Material.PLAYER_HEAD, "gui-items.ninja-decoy", "gui-items.ninja-decoy-lore");
        inv.setItem(14, decoy);
        slotMap.put(14, ABILITY_DECOY);

        ItemStack disguise = createAbilityItem(plugin, Material.WOLF_SPAWN_EGG, "gui-items.ninja-disguise", "gui-items.ninja-disguise-lore-1", "gui-items.ninja-disguise-lore-2");
        inv.setItem(15, disguise);
        slotMap.put(15, ABILITY_DISGUISE);

        guiMappings.put(ninja, slotMap);
        ninja.openInventory(inv);
    }

    public static String getAbilityAtSlot(Player player, int slot) {
        Map<Integer, String> map = guiMappings.get(player);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player player) {
        guiMappings.remove(player);
    }

    public static boolean isNinjaGUI(WerewolfPlugin plugin, String title) {
        if (title == null) return false;
        String configTitle = ChatColor.stripColor(getTitle(plugin));
        return ChatColor.stripColor(title).startsWith(configTitle);
    }

    private static ItemStack createAbilityItem(WerewolfPlugin plugin, Material material, String nameKey, String... loreKeys) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessageUtil().get(nameKey));
            List<String> loreList = new ArrayList<>();
            for (String loreKey : loreKeys) {
                loreList.add(plugin.getMessageUtil().get(loreKey));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
