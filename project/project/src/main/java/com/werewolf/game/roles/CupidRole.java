package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
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
        super("Cupid", Team.NEUTRAL,
                "You are Cupid! On the first night, use your Bow of Love to select two players to become spouses. " +
                        "You may also select yourself as one of the spouses. " +
                        "When one spouse dies, the other dies immediately of a broken heart. " +
                        "Both spouses are revealed in chat when paired. " +
                        "You and your spouse must both stay alive to win together!");
    }

    @Override
    public void onNightStart(Player player) {
        if (!hasPaired) {
            player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&dNight falls! Use your Bow of Love to select two players as spouses."));
            player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "cupid-bow"));
        } else {
            player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&dNight falls! You have already paired your spouses."));
        }
    }

    @Override
    public void onDayStart(Player player) {
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
