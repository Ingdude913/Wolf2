package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TricksterRole extends Role {

    public TricksterRole() {
        super("trickster", Team.GOOD);
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("trickster"));
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart2("trickster"));
        ItemStack armor = ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor");
        player.getInventory().addItem(armor);
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("trickster"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor"));
        return items;
    }

    @Override
    public boolean isTrickster() {
        return true;
    }
}
