package com.werewolf.game.commands;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.arena.ArenaManager;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.MessageUtil;
import com.werewolf.game.util.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WerewolfCommand implements CommandExecutor, TabCompleter {

    private final WerewolfPlugin plugin;

    public WerewolfCommand(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    private String m(String path) {
        return plugin.getMessageUtil().get(path);
    }

    private String m(String path, java.util.Map<String, String> ph) {
        return plugin.getMessageUtil().get(path, ph);
    }

    private void sendNoPerm(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + m("admin.no-permission"));
    }

    private void sendOnlyPlayers(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + m("admin.only-players"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "create":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                if (args.length < 2) { sender.sendMessage(plugin.prefix() + m("admin.create-usage")); return true; }
                handleCreate(sender, args[1]);
                break;
            case "loadworld":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                if (args.length < 2) { sender.sendMessage(plugin.prefix() + m("admin.loadworld-usage")); return true; }
                handleLoadWorld(sender, args[1]);
                break;
            case "setlobby":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                if (!(sender instanceof Player)) { sendOnlyPlayers(sender); return true; }
                handleSetLobby((Player) sender);
                break;
            case "setspawn":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                if (!(sender instanceof Player)) { sendOnlyPlayers(sender); return true; }
                handleSetSpawn((Player) sender);
                break;
            case "forcestart":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleForceStart(sender);
                break;
            case "forcestop":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleForceStop(sender);
                break;
            case "debug":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleDebug(sender);
                break;
            case "setrole":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                if (args.length < 3) { sender.sendMessage(plugin.prefix() + m("admin.setrole-usage")); return true; }
                handleSetRole(sender, args[1], args[2]);
                break;
            case "skipday":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleSkipDay(sender);
                break;
            case "skipnight":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleSkipNight(sender);
                break;
            case "skipelection":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleSkipElection(sender);
                break;
            case "reveal":
                if (!sender.hasPermission("werewolf.admin")) { sendNoPerm(sender); return true; }
                handleReveal(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String worldName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.prefix() + m("admin.only-players-create"));
            return;
        }
        Player player = (Player) sender;
        ArenaManager am = plugin.getArenaManager();

        WorldManager wm = am.getWorldManager();
        if (!wm.worldFolderExists(worldName)) {
            sender.sendMessage(plugin.prefix() + m("admin.world-not-found", MessageUtil.ph("world", worldName)));
            sender.sendMessage(plugin.prefix() + m("admin.world-place", MessageUtil.ph("world", worldName)));
            sender.sendMessage(plugin.prefix() + m("admin.world-available"));
            File worldsFolder = wm.getWorldsFolder();
            File[] dirs = worldsFolder.listFiles(File::isDirectory);
            if (dirs != null && dirs.length > 0) {
                for (File dir : dirs) {
                    sender.sendMessage(m("admin.world-list-entry", MessageUtil.ph("world", dir.getName())));
                }
            } else {
                sender.sendMessage(m("admin.world-none"));
            }
            return;
        }

        World world = wm.loadWorld(worldName);
        if (world == null) {
            sender.sendMessage(plugin.prefix() + m("admin.world-failed", MessageUtil.ph("world", worldName)));
            return;
        }

        am.createGame(worldName);
        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.prefix() + m("admin.create-success", MessageUtil.ph("world", worldName)));
        sender.sendMessage(plugin.prefix() + m("admin.create-teleport"));
        sender.sendMessage(plugin.prefix() + m("admin.create-setspawn"));
        sender.sendMessage(plugin.prefix() + m("admin.create-setlobby"));
    }

    private void handleLoadWorld(CommandSender sender, String worldName) {
        if (!(sender instanceof Player)) {
            sendOnlyPlayers(sender);
            return;
        }
        Player player = (Player) sender;
        ArenaManager am = plugin.getArenaManager();

        WorldManager wm = am.getWorldManager();
        if (!wm.worldFolderExists(worldName)) {
            sender.sendMessage(plugin.prefix() + m("admin.world-not-found", MessageUtil.ph("world", worldName)));
            return;
        }

        World world = wm.loadWorld(worldName);
        if (world == null) {
            sender.sendMessage(plugin.prefix() + m("admin.world-failed", MessageUtil.ph("world", worldName)));
            return;
        }

        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.prefix() + m("admin.loadworld-success", MessageUtil.ph("world", worldName)));
    }

    private void handleSetLobby(Player player) {
        plugin.getArenaManager().setGlobalLobby(player.getLocation());
        player.sendMessage(plugin.prefix() + m("admin.setlobby-success"));
    }

    private void handleSetSpawn(Player player) {
        String worldName = player.getWorld().getName();
        plugin.getArenaManager().setWorldSpawn(worldName, player.getLocation());
        Arena game = plugin.getArenaManager().getGame();
        if (game != null && game.getWorldName().equals(worldName)) {
            game.setSpawnLocation(player.getLocation());
        }
        player.sendMessage(plugin.prefix() + m("admin.setspawn-success", MessageUtil.ph("world", worldName)));
    }

    private void handleForceStart(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.LOBBY) {
            sender.sendMessage(plugin.prefix() + m("admin.game-in-progress"));
            return;
        }
        int minNeeded = game.isDebugMode() ? 1 : 2;
        if (game.getPlayers().size() < minNeeded) {
            sender.sendMessage(plugin.prefix() + m("admin.need-players", MessageUtil.ph("min", String.valueOf(minNeeded))));
            return;
        }
        game.startGame();
        sender.sendMessage(plugin.prefix() + m("admin.force-started"));
    }

    private void handleDebug(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        game.setDebugMode(!game.isDebugMode());
        String state = game.isDebugMode() ? m("admin.debug-on") : m("admin.debug-off");
        sender.sendMessage(plugin.prefix() + m("admin.debug-toggle", MessageUtil.ph("state", state)));
        if (game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + m("admin.debug-hint"));
        }
    }

    private void handleSetRole(CommandSender sender, String playerName, String roleName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + m("admin.setrole-not-online", MessageUtil.ph("player", playerName)));
            return;
        }
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (!game.isPlayerInArena(target)) {
            sender.sendMessage(plugin.prefix() + m("admin.setrole-not-in-game", MessageUtil.ph("player", playerName)));
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + m("admin.setrole-need-debug"));
            return;
        }
        List<String> validRoles = Arrays.asList("werewolf", "villager", "witch", "seer", "hunter", "trickster", "ninja", "mermaid", "masochist", "cupid");
        if (!validRoles.contains(roleName.toLowerCase())) {
            sender.sendMessage(plugin.prefix() + m("admin.setrole-invalid", MessageUtil.ph("roles", String.join(", ", validRoles))));
            return;
        }
        game.forceSetRole(target, roleName);
        sender.sendMessage(plugin.prefix() + m("admin.setrole-success", MessageUtil.ph("player", target.getName(), "role", roleName)));
    }

    private void handleSkipDay(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + m("admin.setrole-need-debug"));
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.DAY) {
            sender.sendMessage(plugin.prefix() + m("admin.skipday-not-day"));
            return;
        }
        game.skipDayFromCommand();
    }

    private void handleSkipNight(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.NIGHT) {
            sender.sendMessage(plugin.prefix() + m("admin.skipnight-not-night"));
            return;
        }
        game.skipNightFromCommand();
    }

    private void handleSkipElection(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.SHERIFF_ELECTION) {
            sender.sendMessage(plugin.prefix() + m("admin.skipelection-not-election"));
            return;
        }
        game.skipElectionFromCommand();
    }

    private void handleReveal(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + m("admin.reveal-need-debug"));
            return;
        }
        game.revealRolesToSender(sender);
    }

    private void handleForceStop(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + m("admin.no-game"));
            return;
        }
        game.forceStop();
        sender.sendMessage(plugin.prefix() + m("admin.force-stopped"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(m("help.header"));
        if (sender.hasPermission("werewolf.admin")) {
            sender.sendMessage(m("help.create"));
            sender.sendMessage(m("help.loadworld"));
            sender.sendMessage(m("help.setlobby"));
            sender.sendMessage(m("help.setspawn"));
            sender.sendMessage(m("help.forcestart"));
            sender.sendMessage(m("help.forcestop"));
            sender.sendMessage(m("help.debug"));
            sender.sendMessage(m("help.setrole"));
            sender.sendMessage(m("help.skipday"));
            sender.sendMessage(m("help.skipnight"));
            sender.sendMessage(m("help.skipelection"));
            sender.sendMessage(m("help.reveal"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission("werewolf.admin")) {
                completions.add("create");
                completions.add("loadworld");
                completions.add("setlobby");
                completions.add("setspawn");
                completions.add("forcestart");
                completions.add("forcestop");
                completions.add("debug");
                completions.add("setrole");
                completions.add("skipday");
                completions.add("skipnight");
                completions.add("skipelection");
                completions.add("reveal");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create") || sub.equals("loadworld")) {
                WorldManager wm = plugin.getArenaManager().getWorldManager();
                File worldsFolder = wm.getWorldsFolder();
                File[] dirs = worldsFolder.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File dir : dirs) {
                        completions.add(dir.getName());
                    }
                }
            } else if (sub.equals("setrole")) {
                Arena game = plugin.getArenaManager().getGame();
                if (game != null) {
                    for (GamePlayer gp : game.getPlayers()) {
                        completions.add(gp.getPlayer().getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setrole")) {
                completions.add("werewolf");
                completions.add("villager");
                completions.add("witch");
                completions.add("seer");
                completions.add("hunter");
                completions.add("trickster");
                completions.add("ninja");
                completions.add("mermaid");
                completions.add("masochist");
                completions.add("cupid");
            }
        }
        return completions;
    }
}
