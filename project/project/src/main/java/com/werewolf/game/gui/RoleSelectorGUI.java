package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
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

public class RoleSelectorGUI {

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();
    private static final Map<Player, Boolean> sheriffEnabledMap = new HashMap<>();

    public static final String ROLE_WEREWOLF = "werewolf";
    public static final String ROLE_VILLAGER = "villager";
    public static final String ROLE_WITCH = "witch";
    public static final String ROLE_SEER = "seer";
    public static final String ROLE_HUNTER = "hunter";
    public static final String ROLE_TRICKSTER = "trickster";
    public static final String ROLE_NINJA = "ninja";
    public static final String ROLE_MERMAID = "mermaid";
    public static final String ROLE_MASOCHIST = "masochist";
    public static final String ROLE_CUPID = "cupid";

    public static String getTitle(WerewolfPlugin plugin) {
        return plugin.getMessageUtil().get("gui.role-selector-title");
    }

    public static void open(WerewolfPlugin plugin, Player player, Map<String, Integer> currentSelection, boolean sheriffEnabled) {
        sheriffEnabledMap.put(player, sheriffEnabled);
        Inventory inv = Bukkit.createInventory(player, 36, getTitle(plugin));
        populateInventory(plugin, inv, player, currentSelection);
        player.openInventory(inv);
    }

    public static void update(WerewolfPlugin plugin, Player player, Map<String, Integer> currentSelection, boolean sheriffEnabled) {
        sheriffEnabledMap.put(player, sheriffEnabled);
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !isRoleSelectorGUI(plugin, player.getOpenInventory().getTitle())) return;
        populateInventory(plugin, inv, player, currentSelection);
        player.updateInventory();
    }

    private static void populateInventory(WerewolfPlugin plugin, Inventory inv, Player player, Map<String, Integer> currentSelection) {
        MessageUtil msg = plugin.getMessageUtil();
        Map<Integer, String> slotMap = new HashMap<>();

        int slot = 10;
        slot = addRole(plugin, inv, slotMap, slot, Material.NETHERITE_AXE, ROLE_WEREWOLF, currentSelection.getOrDefault(ROLE_WEREWOLF, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.STONE_SWORD, ROLE_VILLAGER, currentSelection.getOrDefault(ROLE_VILLAGER, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.POTION, ROLE_WITCH, currentSelection.getOrDefault(ROLE_WITCH, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.BOOK, ROLE_SEER, currentSelection.getOrDefault(ROLE_SEER, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.BOW, ROLE_HUNTER, currentSelection.getOrDefault(ROLE_HUNTER, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.WOLF_SPAWN_EGG, ROLE_TRICKSTER, currentSelection.getOrDefault(ROLE_TRICKSTER, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.ENDER_EYE, ROLE_NINJA, currentSelection.getOrDefault(ROLE_NINJA, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.NAUTILUS_SHELL, ROLE_MERMAID, currentSelection.getOrDefault(ROLE_MERMAID, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.CACTUS, ROLE_MASOCHIST, currentSelection.getOrDefault(ROLE_MASOCHIST, 0));
        slot = addRole(plugin, inv, slotMap, slot, Material.BOW, ROLE_CUPID, currentSelection.getOrDefault(ROLE_CUPID, 0));

        boolean sheriffEnabled = sheriffEnabledMap.getOrDefault(player, true);
        ItemStack sheriffToggle = new ItemStack(sheriffEnabled ? Material.EMERALD : Material.REDSTONE);
        ItemMeta sheriffMeta = sheriffToggle.getItemMeta();
        if (sheriffMeta != null) {
            sheriffMeta.setDisplayName(msg.get(sheriffEnabled ? "gui-items.sheriff-toggle.enabled" : "gui-items.sheriff-toggle.disabled"));
            List<String> lore = new ArrayList<>();
            lore.add(msg.get("gui-items.sheriff-toggle.lore-1"));
            lore.add(msg.get("gui-items.sheriff-toggle.lore-2"));
            lore.add(msg.get("gui-items.sheriff-toggle.lore-3"));
            lore.add(msg.get("gui-items.sheriff-toggle.lore-4"));
            lore.add(msg.get("gui-items.sheriff-toggle.lore-5"));
            sheriffMeta.setLore(lore);
            sheriffToggle.setItemMeta(sheriffMeta);
        }
        inv.setItem(31, sheriffToggle);
        slotMap.put(31, "sheriff-toggle");

        guiMappings.put(player, slotMap);
    }

    private static int addRole(WerewolfPlugin plugin, Inventory inv, Map<Integer, String> slotMap, int slot, Material material, String roleKey, int count) {
        MessageUtil msg = plugin.getMessageUtil();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg.get("gui-items.role-selector." + roleKey));
            List<String> lore = new ArrayList<>();
            lore.add(msg.get("gui-items.role-selector.count", MessageUtil.ph("count", String.valueOf(count))));
            lore.add("");
            lore.add(msg.get("gui-items.role-selector.left-click-add"));
            lore.add(msg.get("gui-items.role-selector.right-click-remove"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
        slotMap.put(slot, roleKey);
        return slot + 2;
    }

    public static String getRoleAtSlot(Player player, int slot) {
        Map<Integer, String> map = guiMappings.get(player);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player player) {
        guiMappings.remove(player);
        sheriffEnabledMap.remove(player);
    }

    public static boolean isRoleSelectorGUI(WerewolfPlugin plugin, String title) {
        if (title == null) return false;
        String configTitle = ChatColor.stripColor(getTitle(plugin));
        return ChatColor.stripColor(title).startsWith(configTitle);
    }
}
