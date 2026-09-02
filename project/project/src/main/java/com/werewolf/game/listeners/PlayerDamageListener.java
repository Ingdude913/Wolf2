package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerDamageListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;

        GamePlayer gp = arena.getGamePlayer(player);
        if (gp == null) return;

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(0.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(target);
        if (arena == null) return;

        GamePlayer targetGp = arena.getGamePlayer(target);
        GamePlayer attackerGp = arena.getGamePlayer(attacker);
        if (targetGp == null || attackerGp == null) return;

        if (arena.getPhase() == Phase.LOBBY || arena.getPhase() == Phase.ENDED || arena.getPhase() == Phase.SHERIFF_ELECTION) {
            event.setCancelled(true);
            return;
        }

        if (!targetGp.isAlive() || !attackerGp.isAlive()) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(0.0);

        if (arena.getPhase() == Phase.DAY) {
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "vote-sword")) {
                arena.castVote(attacker, target);
            }
            return;
        }

        if (arena.getPhase() == Phase.NIGHT) {
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "werewolf-axe")) {
                if (attackerGp.getRole().isWerewolf()) {
                    arena.werewolfKill(attacker, target);
                } else if (attackerGp.getRole().isTrickster()) {
                    attacker.sendMessage(plugin.prefix() + plugin.getMessageUtil().get("game.werewolf-axe-fake"));
                }
                return;
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "witch-poison")) {
                arena.witchPoison(attacker, target);
                return;
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "witch-heal")) {
                arena.witchHeal(attacker, target);
                return;
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "hunter-target")) {
                arena.hunterSelectTarget(attacker, target);
            }
        }
    }
}
