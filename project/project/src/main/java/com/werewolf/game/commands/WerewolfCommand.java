package com.werewolf.game.commands;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.arena.ArenaManager;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww create <world>");
                    return true;
                }
                handleCreate(sender, args[1]);
                break;
            case "loadworld":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww loadworld <world>");
                    return true;
                }
                handleLoadWorld(sender, args[1]);
                break;
            case "setlobby":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                handleSetLobby((Player) sender);
                break;
            case "setspawn":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                handleSetSpawn((Player) sender);
                break;
            case "forcestart":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleForceStart(sender);
                break;
            case "forcestop":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleForceStop(sender);
                break;
            case "debug":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleDebug(sender);
                break;
            case "setrole":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww setrole <player> <role>");
                    return true;
                }
                handleSetRole(sender, args[1], args[2]);
                break;
            case "skipday":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleSkipDay(sender);
                break;
            case "skipnight":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleSkipNight(sender);
                break;
            case "skipelection":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                handleSkipElection(sender);
                break;
            case "reveal":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
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
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can create the game.");
            return;
        }
        Player player = (Player) sender;
        ArenaManager am = plugin.getArenaManager();

        WorldManager wm = am.getWorldManager();
        if (!wm.worldFolderExists(worldName)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "World folder '" + worldName + "' not found!");
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Place your world folder in: plugins/Werewolf/World/" + worldName);
            sender.sendMessage(plugin.prefix() + ChatColor.GRAY + "Available worlds:");
            File worldsFolder = wm.getWorldsFolder();
            File[] dirs = worldsFolder.listFiles(File::isDirectory);
            if (dirs != null && dirs.length > 0) {
                for (File dir : dirs) {
                    sender.sendMessage(ChatColor.GRAY + "  - " + dir.getName());
                }
            } else {
                sender.sendMessage(ChatColor.GRAY + "  (none)");
            }
            return;
        }

        World world = wm.loadWorld(worldName);
        if (world == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Failed to load world '" + worldName + "'. Check the console for errors.");
            return;
        }

        am.createGame(worldName);
        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Game created with world " + worldName + "!");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You have been teleported to the world for setup.");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Use /ww setspawn to set the spawn for this world.");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Use /ww setlobby to set the global lobby where players wait and return after games.");
    }

    private void handleLoadWorld(CommandSender sender, String worldName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
            return;
        }
        Player player = (Player) sender;
        ArenaManager am = plugin.getArenaManager();

        WorldManager wm = am.getWorldManager();
        if (!wm.worldFolderExists(worldName)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "World folder '" + worldName + "' not found!");
            return;
        }

        World world = wm.loadWorld(worldName);
        if (world == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Failed to load world '" + worldName + "'. Check the console for errors.");
            return;
        }

        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "World '" + worldName + "' loaded! You can now use /ww setspawn to set the spawn for this world.");
    }

    private void handleSetLobby(Player player) {
        plugin.getArenaManager().setGlobalLobby(player.getLocation());
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Global lobby location set! Players will be teleported here when they join the server and when a game ends.");
    }

    private void handleSetSpawn(Player player) {
        String worldName = player.getWorld().getName();
        plugin.getArenaManager().setWorldSpawn(worldName, player.getLocation());
        Arena game = plugin.getArenaManager().getGame();
        if (game != null && game.getWorldName().equals(worldName)) {
            game.setSpawnLocation(player.getLocation());
        }
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Spawn location set for world '" + worldName + "'!");
    }

    private void handleForceStart(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists! Use /ww create <world> first.");
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.LOBBY) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Game already in progress!");
            return;
        }
        int minNeeded = game.isDebugMode() ? 1 : 2;
        if (game.getPlayers().size() < minNeeded) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Need at least " + minNeeded + " players to start!");
            return;
        }
        game.startGame();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Game force started!");
    }

    private void handleDebug(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists! Use /ww create <world> first.");
            return;
        }
        game.setDebugMode(!game.isDebugMode());
        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Debug mode: " + (game.isDebugMode() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        if (game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You can now force start with 1 player, assign roles with /ww setrole, skip day with /ww skipday, and see all roles with /ww reveal.");
        }
    }

    private void handleSetRole(CommandSender sender, String playerName, String roleName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Player " + playerName + " is not online!");
            return;
        }
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        if (!game.isPlayerInArena(target)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Player " + playerName + " is not in the game!");
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled first! Use /ww debug");
            return;
        }
        List<String> validRoles = Arrays.asList("werewolf", "villager", "witch", "seer", "hunter", "trickster", "ninja", "mermaid", "masochist", "cupid");
        if (!validRoles.contains(roleName.toLowerCase())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Invalid role! Valid roles: " + String.join(", ", validRoles));
            return;
        }
        game.forceSetRole(target, roleName);
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Set " + target.getName() + "'s role to " + roleName + ".");
    }

    private void handleSkipDay(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled first! Use /ww debug");
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.DAY) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not day time!");
            return;
        }
        game.skipDayFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining day time.");
    }

    private void handleSkipNight(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.NIGHT) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not night time!");
            return;
        }
        game.skipNightFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining night time.");
    }

    private void handleSkipElection(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        if (game.getPhase() != com.werewolf.game.game.Phase.SHERIFF_ELECTION) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not the sheriff election phase!");
            return;
        }
        game.skipElectionFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining election time.");
    }

    private void handleReveal(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        if (!game.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled first! Use /ww debug");
            return;
        }
        game.revealRolesToSender(sender);
    }

    private void handleForceStop(CommandSender sender) {
        Arena game = plugin.getArenaManager().getGame();
        if (game == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No game exists!");
            return;
        }
        game.forceStop();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Game force stopped!");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "===== " + ChatColor.RED + "Werewolf Commands" + ChatColor.DARK_RED + " =====");
        if (sender.hasPermission("werewolf.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/ww create <world>" + ChatColor.GRAY + " - Set up the game world from a world folder");
            sender.sendMessage(ChatColor.GOLD + "/ww loadworld <world>" + ChatColor.GRAY + " - Load a world to set its spawn point");
            sender.sendMessage(ChatColor.GOLD + "/ww setlobby" + ChatColor.GRAY + " - Set the global lobby location (players spawn here)");
            sender.sendMessage(ChatColor.GOLD + "/ww setspawn" + ChatColor.GRAY + " - Set the spawn location for the world you are standing in");
            sender.sendMessage(ChatColor.GOLD + "/ww forcestart" + ChatColor.GRAY + " - Force start the game");
            sender.sendMessage(ChatColor.GOLD + "/ww forcestop" + ChatColor.GRAY + " - Force stop the game");
            sender.sendMessage(ChatColor.GOLD + "/ww debug" + ChatColor.GRAY + " - Toggle debug mode (1-player start, role control)");
            sender.sendMessage(ChatColor.GOLD + "/ww setrole <player> <role>" + ChatColor.GRAY + " - Force-set a player's role (debug only)");
            sender.sendMessage(ChatColor.GOLD + "/ww skipday" + ChatColor.GRAY + " - Skip the remaining day time (debug only)");
            sender.sendMessage(ChatColor.GOLD + "/ww skipnight" + ChatColor.GRAY + " - Skip the remaining night time");
            sender.sendMessage(ChatColor.GOLD + "/ww skipelection" + ChatColor.GRAY + " - Skip the remaining sheriff election time");
            sender.sendMessage(ChatColor.GOLD + "/ww reveal" + ChatColor.GRAY + " - See all players' roles (debug only)");
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
