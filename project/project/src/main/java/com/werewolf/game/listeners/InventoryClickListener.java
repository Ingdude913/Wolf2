package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import com.werewolf.game.gui.NinjaGUI;
import com.werewolf.game.gui.RoleSelectorGUI;
import com.werewolf.game.gui.CupidGUI;
import com.werewolf.game.gui.SeerGUI;
import com.werewolf.game.gui.SheriffGUI;
import com.werewolf.game.gui.MapSelectorGUI;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {

    private final WerewolfPlugin plugin;

    public InventoryClickListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);

        Inventory inv = event.getView().getTopInventory();
        if (inv == null) return;

        String title = event.getView().getTitle();
        if (SeerGUI.isSeerGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer seerGp = arena.getGamePlayer(player);
            if (seerGp == null || !seerGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            Player target = SeerGUI.getPlayerAtSlot(player, slot);
            if (target == null) return;

            arena.seerCheck(player, target);
            player.closeInventory();
        } else if (NinjaGUI.isNinjaGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer ninjaGp = arena.getGamePlayer(player);
            if (ninjaGp == null || !ninjaGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String ability = NinjaGUI.getAbilityAtSlot(player, slot);
            if (ability == null) return;

            arena.ninjaSelectAbility(player, ability);
            player.closeInventory();
        } else if (CupidGUI.isCupidGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer cupidGp = arena.getGamePlayer(player);
            if (cupidGp == null || !cupidGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            Player target = CupidGUI.getPlayerAtSlot(player, slot);
            if (target == null) return;

            arena.cupidSelectSpouse(player, target);
        } else if (RoleSelectorGUI.isRoleSelectorGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;
            if (arena.getPhase() != Phase.LOBBY) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String roleKey = RoleSelectorGUI.getRoleAtSlot(player, slot);
            if (roleKey == null) return;

            if (roleKey.equals("sheriff-toggle")) {
                boolean newState = !arena.isSheriffEnabled();
                arena.setSheriffEnabled(newState);
                player.sendMessage(plugin.prefix() + plugin.getMessageUtil().get("gui-items.sheriff-toggle." + (newState ? "enabled" : "disabled")));
                RoleSelectorGUI.update(plugin, player, arena.getRoleSelection(), newState);
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                arena.adjustRoleSelection(roleKey, 1);
                player.sendMessage(plugin.prefix() + plugin.getMessageUtil().get("game.role-added", MessageUtil.ph("role", roleKey, "count", String.valueOf(arena.getRoleSelection().getOrDefault(roleKey, 0)))));
                RoleSelectorGUI.update(plugin, player, arena.getRoleSelection(), arena.isSheriffEnabled());
            } else if (event.getClick() == ClickType.RIGHT) {
                arena.adjustRoleSelection(roleKey, -1);
                player.sendMessage(plugin.prefix() + plugin.getMessageUtil().get("game.role-removed", MessageUtil.ph("role", roleKey, "count", String.valueOf(arena.getRoleSelection().getOrDefault(roleKey, 0)))));
                RoleSelectorGUI.update(plugin, player, arena.getRoleSelection(), arena.isSheriffEnabled());
            }
        } else if (MapSelectorGUI.isMapSelectorGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;
            if (arena.getPhase() != Phase.LOBBY) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String worldName = MapSelectorGUI.getWorldAtSlot(player, slot);
            if (worldName == null) return;

            arena.selectMap(player, worldName);
            MapSelectorGUI.open(plugin, arena, player);
        } else if (SheriffGUI.isSheriffGUI(plugin, title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer voterGp = arena.getGamePlayer(player);
            if (voterGp == null || !voterGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            Player target = SheriffGUI.getPlayerAtSlot(player, slot);
            if (target == null) return;

            arena.castSheriffVote(player, target);
            player.closeInventory();
        } else if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (SeerGUI.isSeerGUI(plugin, title)) {
            SeerGUI.clearMapping(player);
        } else if (NinjaGUI.isNinjaGUI(plugin, title)) {
            NinjaGUI.clearMapping(player);
        } else if (SheriffGUI.isSheriffGUI(plugin, title)) {
            SheriffGUI.clearMapping(player);
        } else if (CupidGUI.isCupidGUI(plugin, title)) {
            CupidGUI.clearMapping(player);
        } else if (RoleSelectorGUI.isRoleSelectorGUI(plugin, title)) {
            RoleSelectorGUI.clearMapping(player);
        } else if (MapSelectorGUI.isMapSelectorGUI(plugin, title)) {
            MapSelectorGUI.clearMapping(player);
        }
    }
}
