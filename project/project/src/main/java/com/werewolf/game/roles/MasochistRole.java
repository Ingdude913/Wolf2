package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MasochistRole extends Role {

    public MasochistRole() {
        super("masochist", Team.NEUTRAL);
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("masochist"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("masochist"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        return new ArrayList<>();
    }

    @Override
    public boolean isMasochist() {
        return true;
    }
}
