package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NinjaRole extends Role {

    private boolean abilityUsedTonight = false;
    private String selectedAbility = null;

    public NinjaRole() {
        super("ninja", Team.GOOD);
    }

    @Override
    public void onNightStart(Player player) {
        abilityUsedTonight = false;
        selectedAbility = null;
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("ninja"));
        player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("ninja"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-ability"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
        return items;
    }

    public boolean hasUsedAbilityTonight() {
        return abilityUsedTonight;
    }

    public void setAbilityUsedTonight(boolean used) {
        this.abilityUsedTonight = used;
    }

    public String getSelectedAbility() {
        return selectedAbility;
    }

    public void setSelectedAbility(String ability) {
        this.selectedAbility = ability;
    }

    @Override
    public boolean isNinja() {
        return true;
    }
}
