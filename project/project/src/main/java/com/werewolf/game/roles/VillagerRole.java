package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VillagerRole extends Role {

    public VillagerRole() {
        super("villager", Team.GOOD);
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("villager"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("villager"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        return new ArrayList<>();
    }
}
