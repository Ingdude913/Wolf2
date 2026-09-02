package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class Role {

    protected final String roleKey;
    protected final Team team;

    protected Role(String roleKey, Team team) {
        this.roleKey = roleKey;
        this.team = team;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public abstract void onNightStart(Player player);

    public abstract void onDayStart(Player player);

    public abstract List<ItemStack> getRoleItems();

    public String getName() {
        return WerewolfPlugin.getInstance().getMessageUtil().getRoleName(roleKey);
    }

    public Team getTeam() {
        return team;
    }

    public String getDescription() {
        return WerewolfPlugin.getInstance().getMessageUtil().getRoleDescription(roleKey);
    }

    public boolean isWerewolf() {
        return false;
    }

    public boolean isTrickster() {
        return false;
    }

    public boolean isNinja() {
        return false;
    }

    public boolean isMermaid() {
        return false;
    }

    public boolean isMasochist() {
        return false;
    }

    public boolean isCupid() {
        return false;
    }

    public boolean canSeeWerewolves() {
        return false;
    }

    public boolean isGood() {
        return team == Team.GOOD;
    }

    public boolean isBad() {
        return team == Team.BAD;
    }
}
