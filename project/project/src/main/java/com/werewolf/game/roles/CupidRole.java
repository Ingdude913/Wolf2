package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CupidRole extends Role {

    private boolean hasPaired = false;
    private Player spouse1 = null;
    private Player spouse2 = null;

    public CupidRole() {
        super("cupid", Team.NEUTRAL);
    }

    @Override
    public void onNightStart(Player player) {
        if (!hasPaired) {
            player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("cupid"));
            player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "cupid-bow"));
        } else {
            player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().get("roles.cupid.night-start-paired"));
        }
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("cupid"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "cupid-bow"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "cupid-bow"));
        return items;
    }

    public boolean hasPaired() {
        return hasPaired;
    }

    public void setPaired(boolean paired) {
        this.hasPaired = paired;
    }

    public Player getSpouse1() {
        return spouse1;
    }

    public void setSpouse1(Player spouse1) {
        this.spouse1 = spouse1;
    }

    public Player getSpouse2() {
        return spouse2;
    }

    public void setSpouse2(Player spouse2) {
        this.spouse2 = spouse2;
    }

    @Override
    public boolean isCupid() {
        return true;
    }
}
