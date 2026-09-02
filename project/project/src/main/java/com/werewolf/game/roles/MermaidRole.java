package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MermaidRole extends Role {

    private boolean hasSungTonight = false;

    public MermaidRole() {
        super("mermaid", Team.GOOD);
    }

    @Override
    public void onNightStart(Player player) {
        hasSungTonight = false;
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleNightStart("mermaid"));
        player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "mermaid-shell"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + WerewolfPlugin.getInstance().getMessageUtil().getRoleDayStart("mermaid"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "mermaid-shell"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "mermaid-shell"));
        return items;
    }

    public boolean hasSungTonight() {
        return hasSungTonight;
    }

    public void setSungTonight(boolean sung) {
        this.hasSungTonight = sung;
    }

    @Override
    public boolean isMermaid() {
        return true;
    }
}
