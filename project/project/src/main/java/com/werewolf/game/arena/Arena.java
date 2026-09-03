package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.*;
import com.werewolf.game.gui.MapSelectorGUI;
import com.werewolf.game.gui.RoleSelectorGUI;
import com.werewolf.game.gui.SheriffGUI;
import com.werewolf.game.roles.*;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import com.werewolf.game.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class Arena {

    private final WerewolfPlugin plugin;
    private final String name;
    private String worldName;
    private Location lobbyLocation;
    private Location spawnLocation;

    private final Set<GamePlayer> players = new HashSet<>();
    private Phase phase = Phase.LOBBY;
    private int taskId = -1;
    private int phaseTimer = 0;
    private int minPlayers;
    private int dayDuration;
    private int nightDuration;
    private int lobbyDuration;

    private final Map<UUID, Integer> voteCounts = new HashMap<>();
    private final Map<UUID, UUID> hunterTargets = new HashMap<>();
    private final Map<String, Long> abilityCooldowns = new HashMap<>();
    private final Map<UUID, UUID> spouses = new HashMap<>();
    private UUID cupidId = null;

    private int transformCooldown;
    private int ninjaCooldown;
    private int electionDuration;
    private int mermaidFreezeDuration;

    private long mermaidFreezeUntil = 0;

    private boolean debugMode = false;
    private boolean firstDay = true;
    private boolean sheriffEnabled = true;
    private int dayCount = 0;

    private final Map<UUID, Integer> sheriffElectionVotes = new HashMap<>();
    private UUID sheriffId = null;

    private final Map<String, Integer> roleSelection = new HashMap<>();
    private final Map<UUID, Integer> fakeVoteCounts = new HashMap<>();
    private final Map<UUID, String> mapVotes = new HashMap<>();

    private BossBar bossBar = null;
    private int actionBarTaskId = -1;
    private int particleTaskId = -1;
    private final Set<UUID> particleTrailPlayers = new HashSet<>();
    private ScoreboardHelper scoreboardHelper;

    public Arena(WerewolfPlugin plugin, String name, String worldName) {
        this.plugin = plugin;
        this.name = name;
        this.worldName = worldName;
        this.minPlayers = plugin.getConfig().getInt("min-players", 4);
        this.dayDuration = plugin.getConfig().getInt("day-duration", 120);
        this.nightDuration = plugin.getConfig().getInt("night-duration", 60);
        this.lobbyDuration = plugin.getConfig().getInt("lobby-duration", 30);
        this.transformCooldown = plugin.getConfig().getInt("transform-cooldown", 10);
        this.ninjaCooldown = plugin.getConfig().getInt("ninja-cooldown", 15);
        this.electionDuration = plugin.getConfig().getInt("election-duration", 30);
        this.mermaidFreezeDuration = plugin.getConfig().getInt("mermaid-freeze-duration", 15);
        this.scoreboardHelper = new ScoreboardHelper(plugin, this);
        this.scoreboardHelper.setupLobby();

        roleSelection.put("werewolf", 1);
        roleSelection.put("villager", 1);
    }

    private MessageUtil msg() {
        return plugin.getMessageUtil();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getPhaseTimer() {
        return phaseTimer;
    }

    public int getTaskId() {
        return taskId;
    }

    public ScoreboardHelper getScoreboardHelper() {
        return scoreboardHelper;
    }

    public int getDayCount() {
        return dayCount;
    }

    private void sendPhaseTitle(String title, String subtitle) {
        String coloredTitle = ColorUtil.color(title);
        String coloredSubtitle = ColorUtil.color(subtitle);
        for (GamePlayer gp : players) {
            gp.getPlayer().sendTitle(coloredTitle, coloredSubtitle, 10, 40, 10);
        }
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
    }

    public void setSpawnLocation(Location loc) {
        this.spawnLocation = loc;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public Set<GamePlayer> getPlayers() {
        return players;
    }

    public Set<GamePlayer> getAlivePlayers() {
        return players.stream().filter(GamePlayer::isAlive).collect(Collectors.toSet());
    }

    public Set<GamePlayer> getDeadPlayers() {
        return players.stream().filter(gp -> !gp.isAlive()).collect(Collectors.toSet());
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.stream().filter(gp -> gp.getPlayer().getUniqueId().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    public boolean isPlayerInArena(Player player) {
        return getGamePlayer(player) != null;
    }

    public boolean isFull() {
        return players.size() >= 16;
    }

    public void addPlayer(Player player) {
        if (isPlayerInArena(player)) {
            player.sendMessage(plugin.prefix() + msg().get("game.already-in"));
            return;
        }
        if (isFull()) {
            player.sendMessage(plugin.prefix() + msg().get("game.full"));
            return;
        }

        GamePlayer gp = new GamePlayer(player);
        players.add(gp);

        if (phase == Phase.LOBBY) {
            player.sendMessage(plugin.prefix() + msg().get("game.join"));
            broadcast(msg().get("game.join-broadcast", MessageUtil.ph("player", player.getName(), "count", String.valueOf(players.size()), "max", "16")));

            Location lobby = plugin.getArenaManager().getGlobalLobby();
            if (lobby != null) {
                player.teleport(lobby);
            } else if (spawnLocation != null) {
                player.teleport(spawnLocation);
            } else if (lobbyLocation != null) {
                player.teleport(lobbyLocation);
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);

            player.getInventory().setItem(getItemSlot("role-selector"), ItemBuilder.create(plugin, "role-selector"));
            player.getInventory().setItem(getItemSlot("map-selector"), ItemBuilder.create(plugin, "map-selector"));

            if (bossBar != null) {
                bossBar.addPlayer(player);
            }

            scoreboardHelper.updateLobby();

            if (players.size() >= minPlayers && taskId == -1) {
                startLobbyCountdown();
            }
        } else {
            gp.setAlive(false);
            player.sendMessage(plugin.prefix() + msg().get("game.spectator-join"));
            broadcast(msg().get("game.spectator-broadcast", MessageUtil.ph("player", player.getName())));

            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);

            if (bossBar != null) {
                bossBar.addPlayer(player);
            }

            scoreboardHelper.updateGame();
        }
    }

    public void removePlayer(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null) return;

        players.remove(gp);
        particleTrailPlayers.remove(player.getUniqueId());
        voteCounts.remove(player.getUniqueId());
        hunterTargets.remove(player.getUniqueId());
        mapVotes.remove(player.getUniqueId());

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        Location lobby = plugin.getArenaManager().getGlobalLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        broadcast(msg().get("game.leave-broadcast", MessageUtil.ph("player", player.getName(), "count", String.valueOf(players.size()), "max", "16")));

        if (phase == Phase.LOBBY && taskId != -1 && players.size() < minPlayers) {
            cancelTask();
            broadcast(msg().get("game.countdown-cancelled"));
        }

        if (phase == Phase.LOBBY) {
            scoreboardHelper.updateLobby();
        }

        if (phase == Phase.DAY || phase == Phase.NIGHT || phase == Phase.SHERIFF_ELECTION) {
            scoreboardHelper.updateGame();
            if (phase != Phase.SHERIFF_ELECTION) {
                checkWinCondition();
            }
        }
    }

    private void startLobbyCountdown() {
        phaseTimer = lobbyDuration;
        broadcast(msg().get("game.min-reached", MessageUtil.ph("seconds", String.valueOf(lobbyDuration))));

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() < minPlayers) {
                    cancelTask();
                    taskId = -1;
                    broadcast(msg().get("game.countdown-cancelled"));
                    scoreboardHelper.updateLobby();
                    return;
                }
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    startCountdownTitle();
                    return;
                }
                if (phaseTimer <= 10 || phaseTimer % 30 == 0) {
                    broadcast(msg().get("game.countdown-broadcast", MessageUtil.ph("seconds", String.valueOf(phaseTimer))));
                }
                scoreboardHelper.updateLobby();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void startCountdownTitle() {
        final int[] count = {3};
        broadcast(msg().get("game.countdown-broadcast-title"));
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.sendTitle(msg().get("game.countdown-title", MessageUtil.ph("count", String.valueOf(count[0]))), "", 0, 20, 0);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    for (GamePlayer gp : players) {
                        Player p = gp.getPlayer();
                        p.sendTitle(msg().get("game.countdown-title", MessageUtil.ph("count", String.valueOf(count[0]))), "", 0, 20, 0);
                    }
                } else {
                    cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startGame() {
        dayCount = 0;
        loadSelectedWorld();
        assignRoles();
        teleportPlayersToSpawn();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.sendMessage(plugin.prefix() + msg().get("game.role-assigned", MessageUtil.ph("role", gp.getRole().getName())));
            p.sendMessage(plugin.prefix() + msg().get("game.role-description", MessageUtil.ph("description", gp.getRole().getDescription())));
        }
        startParticleTask();
        if (sheriffEnabled) {
            startSheriffElection();
        } else {
            broadcast(msg().get("game.sheriff-disabled-skip"));
            firstDay = true;
            giveDayItems();
            scoreboardHelper.setupGame();
            for (GamePlayer gp : getAlivePlayers()) {
                gp.getRole().onDayStart(gp.getPlayer());
            }
            startDayPhase();
        }
    }

    private void startSheriffElection() {
        phase = Phase.SHERIFF_ELECTION;
        phaseTimer = electionDuration;
        setWorldTime(6000);
        sendPhaseTitle("&6Sheriff Election", "&7Day " + dayCount);
        broadcast(msg().get("game.sheriff-header"));
        broadcast(msg().get("game.sheriff-instruct-1"));
        broadcast(msg().get("game.sheriff-instruct-2"));

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            p.getInventory().clear();
            p.getInventory().setItem(getItemSlot("vote-sheriff"), ItemBuilder.create(plugin, "vote-sheriff"));
            giveInfoItems(gp);
        }

        createBossBar(msg().get("game.boss-bar-election"), BarColor.YELLOW);
        scoreboardHelper.setupGame();

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endSheriffElection();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(msg().get("game.sheriff-timer", MessageUtil.ph("seconds", String.valueOf(phaseTimer))));
                }
                updateBossBar();
                scoreboardHelper.updateGame();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endSheriffElection() {
        List<UUID> topVoted = new ArrayList<>();
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : sheriffElectionVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                topVoted.clear();
                topVoted.add(entry.getKey());
            } else if (entry.getValue() == maxVotes) {
                topVoted.add(entry.getKey());
            }
        }
        sheriffElectionVotes.clear();

        if (topVoted.isEmpty() || maxVotes == 0) {
            broadcast(msg().get("game.sheriff-tie"));
        } else {
            UUID electedId = topVoted.size() == 1 ? topVoted.get(0) : topVoted.get(new Random().nextInt(topVoted.size()));
            Player sheriff = Bukkit.getPlayer(electedId);
            if (sheriff != null) {
                GamePlayer sheriffGp = getGamePlayer(sheriff);
                if (sheriffGp != null) {
                    sheriffGp.setSheriff(true);
                    sheriffId = electedId;
                    broadcast(msg().get("game.sheriff-elected-header"));
                    broadcast(msg().get("game.sheriff-elected", MessageUtil.ph("player", sheriff.getName())));
                    sheriff.sendMessage(plugin.prefix() + msg().get("game.sheriff-you"));
                }
            }
        }

        firstDay = true;
        broadcast(msg().get("game.game-started"));
        giveDayItems();
        scoreboardHelper.updateGame();
        for (GamePlayer gp : getAlivePlayers()) {
            gp.getRole().onDayStart(gp.getPlayer());
        }
        startDayPhase();
    }

    public void castSheriffVote(Player voter, Player target) {
        GamePlayer voterGp = getGamePlayer(voter);
        GamePlayer targetGp = getGamePlayer(target);
        if (voterGp == null || targetGp == null) return;
        if (phase != Phase.SHERIFF_ELECTION) return;
        if (!voterGp.isAlive() || !targetGp.isAlive()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.sheriff-dead-vote"));
            return;
        }
        if (voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.sheriff-already-voted"));
            return;
        }
        voterGp.setVoted(true);
        voterGp.setVotedFor(target);
        sheriffElectionVotes.merge(target.getUniqueId(), 1, Integer::sum);
        voter.sendMessage(plugin.prefix() + msg().get("game.sheriff-vote-cast", MessageUtil.ph("target", target.getName())));
        broadcast(msg().get("game.sheriff-vote-broadcast", MessageUtil.ph("player", voter.getName())));
    }

    public void revokeSheriffVote(Player voter) {
        GamePlayer voterGp = getGamePlayer(voter);
        if (voterGp == null || !voterGp.isAlive()) return;
        if (phase != Phase.SHERIFF_ELECTION) return;
        if (!voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.sheriff-not-voted"));
            return;
        }
        Player target = voterGp.getVotedFor();
        if (target != null) {
            sheriffElectionVotes.merge(target.getUniqueId(), -1, Integer::sum);
            if (sheriffElectionVotes.getOrDefault(target.getUniqueId(), 0) <= 0) {
                sheriffElectionVotes.remove(target.getUniqueId());
            }
        }
        voterGp.resetVote();
        voter.sendMessage(plugin.prefix() + msg().get("game.sheriff-revoke"));
    }

    public Map<String, Integer> getRoleSelection() {
        return roleSelection;
    }

    public void adjustRoleSelection(String roleKey, int delta) {
        int current = roleSelection.getOrDefault(roleKey, 0);
        int newValue = Math.max(0, current + delta);
        if (!debugMode && (roleKey.equals("werewolf") || roleKey.equals("villager")) && newValue < 1) {
            return;
        }
        roleSelection.put(roleKey, newValue);
    }

    public void openRoleSelector(Player player) {
        RoleSelectorGUI.open(plugin, player, roleSelection, sheriffEnabled);
    }

    public boolean isSheriffEnabled() {
        return sheriffEnabled;
    }

    public void setSheriffEnabled(boolean enabled) {
        this.sheriffEnabled = enabled;
    }

    private void assignRoles() {
        List<GamePlayer> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList);

        int total = playerList.size();

        int werewolfCount = roleSelection.getOrDefault("werewolf", 0);
        int villagerCount = roleSelection.getOrDefault("villager", 0);
        int witchCount = roleSelection.getOrDefault("witch", 0);
        int seerCount = roleSelection.getOrDefault("seer", 0);
        int hunterCount = roleSelection.getOrDefault("hunter", 0);
        int tricksterCount = roleSelection.getOrDefault("trickster", 0);
        int ninjaCount = roleSelection.getOrDefault("ninja", 0);
        int mermaidCount = roleSelection.getOrDefault("mermaid", 0);
        int masochistCount = roleSelection.getOrDefault("masochist", 0);
        int cupidCount = roleSelection.getOrDefault("cupid", 0);

        int selectedTotal = werewolfCount + villagerCount + witchCount + seerCount + hunterCount + tricksterCount + ninjaCount + mermaidCount + masochistCount + cupidCount;
        if (selectedTotal > total) {
            int overflow = selectedTotal - total;
            if (masochistCount >= overflow) { masochistCount -= overflow; overflow = 0; } else { overflow -= masochistCount; masochistCount = 0; }
            if (overflow > 0 && mermaidCount >= overflow) { mermaidCount -= overflow; overflow = 0; } else { overflow -= mermaidCount; mermaidCount = 0; }
            if (overflow > 0 && ninjaCount >= overflow) { ninjaCount -= overflow; overflow = 0; } else { overflow -= ninjaCount; ninjaCount = 0; }
            if (overflow > 0 && tricksterCount >= overflow) { tricksterCount -= overflow; overflow = 0; } else { overflow -= tricksterCount; tricksterCount = 0; }
            if (overflow > 0 && hunterCount >= overflow) { hunterCount -= overflow; overflow = 0; } else { overflow -= hunterCount; hunterCount = 0; }
            if (overflow > 0 && seerCount >= overflow) { seerCount -= overflow; overflow = 0; } else { overflow -= seerCount; seerCount = 0; }
            if (overflow > 0 && witchCount >= overflow) { witchCount -= overflow; overflow = 0; } else { overflow -= witchCount; witchCount = 0; }
            if (overflow > 0 && villagerCount >= overflow) { villagerCount -= overflow; overflow = 0; } else { overflow -= villagerCount; villagerCount = 0; }
            if (overflow > 0 && werewolfCount >= overflow) { werewolfCount -= overflow; overflow = 0; } else { overflow -= werewolfCount; werewolfCount = 0; }
            if (overflow > 0 && cupidCount >= overflow) { cupidCount -= overflow; overflow = 0; } else { overflow -= cupidCount; cupidCount = 0; }
        }

        if (!debugMode) {
            if (werewolfCount < 1) werewolfCount = 1;
            if (villagerCount < 1) villagerCount = 1;
        }

        int index = 0;
        for (int i = 0; i < werewolfCount && index < total; i++) {
            playerList.get(index++).setRole(new WerewolfRole());
        }
        for (int i = 0; i < tricksterCount && index < total; i++) {
            playerList.get(index++).setRole(new TricksterRole());
        }
        for (int i = 0; i < witchCount && index < total; i++) {
            playerList.get(index++).setRole(new WitchRole());
        }
        for (int i = 0; i < seerCount && index < total; i++) {
            playerList.get(index++).setRole(new SeerRole());
        }
        for (int i = 0; i < hunterCount && index < total; i++) {
            playerList.get(index++).setRole(new HunterRole());
        }
        for (int i = 0; i < ninjaCount && index < total; i++) {
            playerList.get(index++).setRole(new NinjaRole());
        }
        for (int i = 0; i < mermaidCount && index < total; i++) {
            playerList.get(index++).setRole(new MermaidRole());
        }
        for (int i = 0; i < masochistCount && index < total; i++) {
            playerList.get(index++).setRole(new MasochistRole());
        }
        for (int i = 0; i < cupidCount && index < total; i++) {
            playerList.get(index++).setRole(new CupidRole());
        }
        while (index < total) {
            playerList.get(index++).setRole(new VillagerRole());
        }

        List<String> werewolfNames = new ArrayList<>();
        for (GamePlayer gp : players) {
            if (gp.getRole().isWerewolf() || gp.getRole().isTrickster()) {
                werewolfNames.add(gp.getPlayer().getName());
            }
        }
        for (GamePlayer gp : players) {
            if (gp.getRole().canSeeWerewolves()) {
                Player p = gp.getPlayer();
                p.sendMessage(plugin.prefix() + msg().get("game.wolf-team-list", MessageUtil.ph("names", String.join(", ", werewolfNames))));
            }
        }

        for (GamePlayer gp : players) {
            if (gp.getRole().isCupid()) {
                cupidId = gp.getPlayer().getUniqueId();
            }
        }
    }

    private void teleportPlayersToSpawn() {
        if (spawnLocation != null) {
            for (GamePlayer gp : players) {
                gp.getPlayer().teleport(spawnLocation);
            }
        }
    }

    private void loadSelectedWorld() {
        String selectedWorld = getSelectedMap();
        if (selectedWorld == null) return;
        if (selectedWorld.equals(worldName) && spawnLocation != null) return;
        World world = plugin.getArenaManager().getWorldManager().loadWorld(selectedWorld);
        if (world == null) {
            broadcast(msg().get("game.map-failed", MessageUtil.ph("world", selectedWorld)));
            return;
        }
        worldName = selectedWorld;
        Location spawn = plugin.getArenaManager().getWorldSpawn(selectedWorld);
        if (spawn != null) {
            spawn.setWorld(world);
            spawnLocation = spawn;
        } else {
            spawnLocation = world.getSpawnLocation();
        }
        broadcast(msg().get("game.map-selected", MessageUtil.ph("world", selectedWorld)));
    }

    private String getSelectedMap() {
        Map<String, Integer> counts = new HashMap<>();
        for (String world : mapVotes.values()) {
            counts.merge(world, 1, Integer::sum);
        }
        String winner = null;
        int maxVotes = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }
        if (winner == null) {
            List<String> worlds = plugin.getArenaManager().getAvailableWorlds();
            if (!worlds.isEmpty()) {
                winner = worlds.get(new Random().nextInt(worlds.size()));
            }
        }
        return winner;
    }

    public void voteForMap(Player player, String worldName) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        if (phase != Phase.LOBBY) {
            player.sendMessage(plugin.prefix() + msg().get("game.map-vote-not-lobby"));
            return;
        }
        List<String> available = plugin.getArenaManager().getAvailableWorlds();
        if (!available.contains(worldName)) {
            player.sendMessage(plugin.prefix() + msg().get("game.map-vote-not-available"));
            return;
        }
        mapVotes.put(player.getUniqueId(), worldName);
        player.sendMessage(plugin.prefix() + msg().get("game.map-vote-cast", MessageUtil.ph("world", worldName)));
        broadcast(msg().get("game.map-vote-broadcast", MessageUtil.ph("player", player.getName(), "world", worldName)));
    }

    public String getPlayerMapVote(Player player) {
        return mapVotes.get(player.getUniqueId());
    }

    public Map<UUID, String> getMapVotes() {
        return mapVotes;
    }

    public void openMapSelector(Player player) {
        MapSelectorGUI.open(plugin, this, player);
    }

    private void giveDayItems() {
        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            p.getInventory().clear();
            if (!firstDay) {
                p.getInventory().setItem(getItemSlot("vote-sword"), ItemBuilder.create(plugin, "vote-sword"));
                p.getInventory().setItem(getItemSlot("revoke-vote"), ItemBuilder.create(plugin, "revoke-vote"));
            }
            p.getInventory().setItem(getItemSlot("skip-day"), ItemBuilder.create(plugin, "skip-day"));
            giveInfoItems(gp);
        }
    }

    private void giveInfoItems(GamePlayer gp) {
        Player p = gp.getPlayer();
        p.getInventory().setItem(getItemSlot("setup-info"), ItemBuilder.create(plugin, "setup-info"));
        p.getInventory().setItem(getItemSlot("role-info-book"), ItemBuilder.create(plugin, "role-info-book"));
        if (gp.getRole().isWerewolf()) {
            p.getInventory().setItem(getItemSlot("wolf-team"), ItemBuilder.create(plugin, "wolf-team"));
        }
    }

    private int getItemSlot(String itemKey) {
        return plugin.getConfig().getInt("items." + itemKey + ".slot", 0);
    }

    private void startDayPhase() {
        phase = Phase.DAY;
        phaseTimer = dayDuration;
        setWorldTime(6000);
        dayCount++;
        sendPhaseTitle("&6Day", "&7Day " + dayCount);

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            gp.setTransformed(false);
        }
        particleTrailPlayers.clear();

        createBossBar(msg().get("game.boss-bar-day"), BarColor.YELLOW);
        startActionBar();

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endDayPhase();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(msg().get("game.day-ends-in", MessageUtil.ph("seconds", String.valueOf(phaseTimer))));
                }
                updateBossBar();
                scoreboardHelper.updateGame();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endDayPhase() {
        processVotes();
        if (checkWinCondition()) return;
        startNightPhase();
    }

    private void processVotes() {
        for (Map.Entry<UUID, Integer> entry : fakeVoteCounts.entrySet()) {
            voteCounts.merge(entry.getKey(), -entry.getValue(), Integer::sum);
            if (voteCounts.getOrDefault(entry.getKey(), 0) <= 0) {
                voteCounts.remove(entry.getKey());
            }
        }
        fakeVoteCounts.clear();
        scoreboardHelper.updateVotes(voteCounts);
        if (voteCounts.isEmpty()) {
            broadcast(msg().get("game.no-votes"));
            return;
        }
        List<UUID> topVoted = new ArrayList<>();
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                topVoted.clear();
                topVoted.add(entry.getKey());
            } else if (entry.getValue() == maxVotes) {
                topVoted.add(entry.getKey());
            }
        }
        voteCounts.clear();
        for (GamePlayer gp : players) {
            gp.resetVote();
        }
        scoreboardHelper.updateVotes(voteCounts);
        if (topVoted.isEmpty() || maxVotes == 0) {
            broadcast(msg().get("game.vote-tied"));
            return;
        }
        UUID mostVoted = topVoted.size() == 1 ? topVoted.get(0) : topVoted.get(new Random().nextInt(topVoted.size()));
        Player eliminated = Bukkit.getPlayer(mostVoted);
        if (eliminated == null) return;
        GamePlayer gp = getGamePlayer(eliminated);
        if (gp == null || !gp.isAlive()) return;
        if (gp.getRole().isMasochist() && !debugMode) {
            broadcast(msg().get("game.masochist-header"));
            broadcast(msg().get("game.masochist-win", MessageUtil.ph("player", eliminated.getName())));
            endGame(msg().get("win.spouses-team"), eliminated.getName() + " (Masochist) " + msg().raw("game.masochist-win", MessageUtil.ph("player", eliminated.getName())));
            return;
        }
        eliminatePlayer(gp, "voted out by the village");
    }

    public void castVote(Player voter, Player target) {
        GamePlayer voterGp = getGamePlayer(voter);
        GamePlayer targetGp = getGamePlayer(target);
        if (voterGp == null || targetGp == null) return;
        if (!voterGp.isAlive() || !targetGp.isAlive()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-dead"));
            return;
        }
        if (voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-already"));
            return;
        }
        if (voterGp.getRole().isMasochist()) {
            voterGp.setVoted(true);
            voterGp.setVotedFor(target);
            voteCounts.merge(target.getUniqueId(), 1, Integer::sum);
            fakeVoteCounts.merge(target.getUniqueId(), 1, Integer::sum);
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-cast", MessageUtil.ph("target", target.getName())));
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-fake"));
            broadcast(msg().get("game.vote-broadcast", MessageUtil.ph("player", voter.getName(), "votes", String.valueOf(voteCounts.getOrDefault(target.getUniqueId(), 0)), "target", target.getName())));
            scoreboardHelper.updateVotes(voteCounts);
            return;
        }
        voterGp.setVoted(true);
        voterGp.setVotedFor(target);
        int voteWeight = voterGp.isSheriff() ? 2 : 1;
        voteCounts.merge(target.getUniqueId(), voteWeight, Integer::sum);
        if (voterGp.isSheriff()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-cast-sheriff", MessageUtil.ph("target", target.getName())));
        } else {
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-cast", MessageUtil.ph("target", target.getName())));
        }
        broadcast(msg().get("game.vote-broadcast", MessageUtil.ph("player", voter.getName(), "votes", String.valueOf(voteCounts.getOrDefault(target.getUniqueId(), 0)), "target", target.getName())));
        scoreboardHelper.updateVotes(voteCounts);
    }

    public void revokeVote(Player voter) {
        GamePlayer voterGp = getGamePlayer(voter);
        if (voterGp == null || !voterGp.isAlive()) return;
        if (!voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + msg().get("game.vote-not-voted"));
            return;
        }
        Player target = voterGp.getVotedFor();
        if (target != null && voterGp.getRole().isMasochist()) {
            voteCounts.merge(target.getUniqueId(), -1, Integer::sum);
            fakeVoteCounts.merge(target.getUniqueId(), -1, Integer::sum);
            if (voteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                voteCounts.remove(target.getUniqueId());
            }
            if (fakeVoteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                fakeVoteCounts.remove(target.getUniqueId());
            }
        } else if (target != null) {
            int voteWeight = voterGp.isSheriff() ? 2 : 1;
            voteCounts.merge(target.getUniqueId(), -voteWeight, Integer::sum);
            if (voteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                voteCounts.remove(target.getUniqueId());
            }
        }
        voterGp.resetVote();
        voter.sendMessage(plugin.prefix() + msg().get("game.vote-revoked"));
        scoreboardHelper.updateVotes(voteCounts);
    }

    private void startNightPhase() {
        phase = Phase.NIGHT;
        phaseTimer = nightDuration;
        setWorldTime(18000);
        sendPhaseTitle("&5Night", "&7Day " + dayCount);

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            gp.setTransformed(false);
            p.getInventory().clear();
            gp.getRole().onNightStart(p);
            giveInfoItems(gp);
        }
        particleTrailPlayers.clear();

        createBossBar(msg().get("game.boss-bar-night"), BarColor.PURPLE);

        broadcast(msg().get("game.night-falls"));

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endNightPhase();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(msg().get("game.night-ends-in", MessageUtil.ph("seconds", String.valueOf(phaseTimer))));
                }
                updateBossBar();
                scoreboardHelper.updateGame();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endNightPhase() {
        processNightDeaths();
        if (checkWinCondition()) return;
        firstDay = false;
        startDayPhase();
        giveDayItems();
        for (GamePlayer gp : getAlivePlayers()) {
            gp.getRole().onDayStart(gp.getPlayer());
        }
        processHunterRevenge();
    }

    private final List<GamePlayer> pendingNightDeaths = new ArrayList<>();

    public void addNightDeath(GamePlayer gp) {
        if (gp != null && gp.isAlive() && !pendingNightDeaths.contains(gp)) {
            pendingNightDeaths.add(gp);
        }
    }

    private void processNightDeaths() {
        for (GamePlayer gp : pendingNightDeaths) {
            if (gp.isAlive()) {
                eliminatePlayer(gp, "killed during the night");
            }
        }
        pendingNightDeaths.clear();
    }

    private void processHunterRevenge() {
        Iterator<Map.Entry<UUID, UUID>> it = hunterTargets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player hunter = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue());
            if (hunter != null && target != null) {
                GamePlayer hunterGp = getGamePlayer(hunter);
                GamePlayer targetGp = getGamePlayer(target);
                if (hunterGp != null && !hunterGp.isAlive() && targetGp != null && targetGp.isAlive()) {
                    broadcast(msg().get("game.killed-hunter-revenge", MessageUtil.ph("target", target.getName())));
                    eliminatePlayer(targetGp, "killed by the Hunter's revenge");
                }
            }
            it.remove();
        }
        if (checkWinCondition()) return;
    }

    public void eliminatePlayer(GamePlayer gp, String reason) {
        if (!gp.isAlive()) return;
        gp.setAlive(false);
        particleTrailPlayers.remove(gp.getPlayer().getUniqueId());
        Player p = gp.getPlayer();
        p.setGameMode(GameMode.SPECTATOR);
        p.getInventory().clear();
        broadcast(msg().get("game.eliminated", MessageUtil.ph("player", p.getName(), "reason", reason)));

        if (gp.getRole() instanceof HunterRole) {
            HunterRole hunter = (HunterRole) gp.getRole();
            Player target = hunter.getTarget();
            if (target != null) {
                hunterTargets.put(p.getUniqueId(), target.getUniqueId());
                broadcast(msg().get("game.hunter-target-info", MessageUtil.ph("player", p.getName(), "target", target.getName())));
            }
        }

        UUID partnerId = spouses.get(p.getUniqueId());
        if (partnerId != null) {
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null) {
                GamePlayer partnerGp = getGamePlayer(partner);
                if (partnerGp != null && partnerGp.isAlive()) {
                    broadcast(msg().get("game.spouse-broken-heart", MessageUtil.ph("player", p.getName(), "partner", partner.getName())));
                    eliminatePlayer(partnerGp, msg().get("game.spouse-died-broken-heart"));
                }
            }
            spouses.remove(partnerId);
            spouses.remove(p.getUniqueId());
        }
    }

    public void cupidSelectSpouse(Player cupid, Player target) {
        GamePlayer cupidGp = getGamePlayer(cupid);
        GamePlayer targetGp = getGamePlayer(target);
        if (cupidGp == null || targetGp == null) return;
        CupidRole cupidRole = cupidGp.asCupid();
        if (cupidRole == null) return;
        if (cupidRole.hasPaired()) {
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-already-paired"));
            return;
        }
        if (!targetGp.isAlive()) {
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-dead-target"));
            return;
        }

        if (cupidRole.getSpouse1() != null && cupidRole.getSpouse1().getUniqueId().equals(target.getUniqueId())) {
            cupidRole.setSpouse1(null);
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-deselect", MessageUtil.ph("target", target.getName())));
            com.werewolf.game.gui.CupidGUI.open(plugin, this, cupid);
            return;
        }
        if (cupidRole.getSpouse2() != null && cupidRole.getSpouse2().getUniqueId().equals(target.getUniqueId())) {
            cupidRole.setSpouse2(null);
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-deselect", MessageUtil.ph("target", target.getName())));
            com.werewolf.game.gui.CupidGUI.open(plugin, this, cupid);
            return;
        }

        if (cupidRole.getSpouse1() == null) {
            cupidRole.setSpouse1(target);
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-first-selected", MessageUtil.ph("target", target.getName())));
            cupid.closeInventory();
            com.werewolf.game.gui.CupidGUI.open(plugin, this, cupid);
            return;
        }
        if (cupidRole.getSpouse2() != null) {
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-already-paired"));
            return;
        }
        Player first = cupidRole.getSpouse1();
        if (target.getUniqueId().equals(first.getUniqueId())) {
            cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-already-first"));
            return;
        }
        cupidRole.setSpouse2(target);
        cupidRole.setPaired(true);
        spouses.put(first.getUniqueId(), target.getUniqueId());
        spouses.put(target.getUniqueId(), first.getUniqueId());
        cupid.getInventory().removeItem(ItemBuilder.create(plugin, "cupid-bow"));
        cupid.closeInventory();
        broadcast(msg().get("game.cupid-arrow-header"));
        broadcast(msg().get("game.cupid-arrow-broadcast", MessageUtil.ph("spouse1", first.getName(), "spouse2", target.getName())));
        first.sendMessage(plugin.prefix() + msg().get("game.cupid-spouse-notify", MessageUtil.ph("partner", target.getName())));
        target.sendMessage(plugin.prefix() + msg().get("game.cupid-spouse-notify", MessageUtil.ph("partner", first.getName())));
        cupid.sendMessage(plugin.prefix() + msg().get("game.cupid-paired-self", MessageUtil.ph("spouse1", first.getName(), "spouse2", target.getName())));
    }

    public void werewolfKill(Player killer, Player target) {
        GamePlayer killerGp = getGamePlayer(killer);
        GamePlayer targetGp = getGamePlayer(target);
        if (killerGp == null || targetGp == null) return;
        if (!killerGp.getRole().isWerewolf()) return;
        if (!killerGp.isAlive() || !targetGp.isAlive()) return;
        if (targetGp.getRole().isWerewolf()) {
            killer.sendMessage(plugin.prefix() + msg().get("game.werewolf-cannot-kill-wolf"));
            return;
        }
        eliminatePlayer(targetGp, "killed by a werewolf");
        killer.sendMessage(plugin.prefix() + msg().get("game.killed-werewolf", MessageUtil.ph("target", target.getName())));
        checkWinCondition();
    }

    public void witchPoison(Player witch, Player target) {
        GamePlayer witchGp = getGamePlayer(witch);
        GamePlayer targetGp = getGamePlayer(target);
        if (witchGp == null || targetGp == null) return;
        WitchRole witchRole = witchGp.asWitch();
        if (witchRole == null) return;
        if (witchRole.isPoisonUsed()) {
            witch.sendMessage(plugin.prefix() + msg().get("game.witch-poison-used"));
            return;
        }
        witchRole.usePoison();
        addNightDeath(targetGp);
        witch.getInventory().removeItem(ItemBuilder.create(plugin, "witch-poison"));
        witch.sendMessage(plugin.prefix() + msg().get("game.witch-poison-success", MessageUtil.ph("target", target.getName())));
    }

    public void witchHeal(Player witch, Player target) {
        GamePlayer witchGp = getGamePlayer(witch);
        GamePlayer targetGp = getGamePlayer(target);
        if (witchGp == null || targetGp == null) return;
        WitchRole witchRole = witchGp.asWitch();
        if (witchRole == null) return;
        if (witchRole.isHealUsed()) {
            witch.sendMessage(plugin.prefix() + msg().get("game.witch-heal-used"));
            return;
        }
        witchRole.useHeal();
        pendingNightDeaths.remove(targetGp);
        target.setHealth(20);
        witch.getInventory().removeItem(ItemBuilder.create(plugin, "witch-heal"));
        witch.sendMessage(plugin.prefix() + msg().get("game.witch-heal-success", MessageUtil.ph("target", target.getName())));
    }

    public void seerCheck(Player seer, Player target) {
        GamePlayer seerGp = getGamePlayer(seer);
        GamePlayer targetGp = getGamePlayer(target);
        if (seerGp == null || targetGp == null) return;
        SeerRole seerRole = seerGp.asSeer();
        if (seerRole == null) return;
        if (seerRole.hasCheckedTonight()) {
            seer.sendMessage(plugin.prefix() + msg().get("game.seer-already-checked"));
            return;
        }
        seerRole.setCheckedTonight(true);
        Team team = targetGp.getRole().getTeam();
        String teamName;
        if (targetGp.getRole().isTrickster()) {
            teamName = msg().get("game.seer-team-bad");
        } else if (team == Team.BAD) {
            teamName = msg().get("game.seer-team-bad");
        } else {
            teamName = msg().get("game.seer-team-good");
        }
        seer.sendMessage(plugin.prefix() + msg().get("game.seer-result", MessageUtil.ph("target", target.getName(), "team", teamName)));
    }

    public void seerLampSwap(Player seer) {
        GamePlayer seerGp = getGamePlayer(seer);
        if (seerGp == null || !seerGp.isAlive()) return;
        SeerRole seerRole = seerGp.asSeer();
        if (seerRole == null) return;
        if (seerRole.hasUsedLampTonight()) {
            seer.sendMessage(plugin.prefix() + msg().get("game.seer-lamp-used"));
            return;
        }
        Player furthest = null;
        double maxDist = -1;
        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            if (p.getUniqueId().equals(seer.getUniqueId())) continue;
            double dist = p.getLocation().distanceSquared(seer.getLocation());
            if (dist > maxDist) {
                maxDist = dist;
                furthest = p;
            }
        }
        if (furthest == null) {
            seer.sendMessage(plugin.prefix() + msg().get("game.seer-lamp-no-target"));
            return;
        }
        seerRole.setUsedLampTonight(true);
        Location seerLoc = seer.getLocation().clone();
        Location targetLoc = furthest.getLocation().clone();
        seer.teleport(targetLoc);
        furthest.teleport(seerLoc);
        seer.sendMessage(plugin.prefix() + msg().get("game.seer-lamp-success", MessageUtil.ph("target", furthest.getName())));
        furthest.sendMessage(plugin.prefix() + msg().get("game.seer-lamp-victim"));
    }

    public void hunterSelectTarget(Player hunter, Player target) {
        GamePlayer hunterGp = getGamePlayer(hunter);
        GamePlayer targetGp = getGamePlayer(target);
        if (hunterGp == null || targetGp == null) return;
        HunterRole hunterRole = hunterGp.asHunter();
        if (hunterRole == null) return;
        if (hunterRole.isTargetLocked()) {
            hunter.sendMessage(plugin.prefix() + msg().get("game.hunter-target-locked"));
            return;
        }
        hunterRole.setTarget(target);
        hunter.sendMessage(plugin.prefix() + msg().get("game.hunter-target-set", MessageUtil.ph("target", target.getName())));
    }

    public void werewolfTransform(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        if (!gp.getRole().isWerewolf() && !gp.getRole().isTrickster()) return;
        String cooldownKey = player.getUniqueId() + ":transform";
        if (isOnCooldown(cooldownKey, transformCooldown, player)) return;
        setCooldown(cooldownKey);
        PlayerInventory inv = player.getInventory();

        if (gp.isTransformed()) {
            inv.setHelmet(null);
            inv.setChestplate(null);
            inv.setLeggings(null);
            inv.setBoots(null);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            inv.removeItem(ItemBuilder.create(plugin, "werewolf-axe"));
            gp.setTransformed(false);
            particleTrailPlayers.remove(player.getUniqueId());
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            player.sendMessage(plugin.prefix() + msg().get("game.werewolf-untransform"));
            return;
        }

        ItemStack helmet = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_HELMET));
        ItemStack chestplate = ItemBuilder.create(plugin, "werewolf-armor");
        ItemStack leggings = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_LEGGINGS));
        ItemStack boots = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_BOOTS));
        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);

        ItemStack axe = ItemBuilder.create(plugin, "werewolf-axe");
        if (gp.getRole().isTrickster()) {
            axe = ItemBuilder.rename(axe, "&4&lFake Werewolf Axe &7(Cannot kill)");
        }
        if (!inv.contains(axe)) {
            inv.addItem(axe);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        particleTrailPlayers.add(player.getUniqueId());
        gp.setTransformed(true);
        player.sendMessage(plugin.prefix() + msg().get("game.werewolf-transform"));
    }

    private boolean checkWinCondition() {
        if (debugMode) return false;
        if (players.isEmpty()) return true;
        Set<GamePlayer> alive = getAlivePlayers();
        if (alive.isEmpty()) {
            endGame(msg().get("win.draw-team"), msg().get("win.draw"));
            return true;
        }

        boolean badAlive = alive.stream().anyMatch(gp -> gp.getRole().isBad());
        boolean goodAlive = alive.stream().anyMatch(gp -> gp.getRole().isGood());

        if (!badAlive && !goodAlive) {
            if (areSpousesAlive()) {
                endGame(msg().get("win.spouses-team"), msg().get("win.spouses-all-gone"));
                return true;
            }
            endGame(msg().get("win.neutral-team"), msg().get("win.neutral"));
            return true;
        }
        if (!badAlive) {
            if (areSpousesAlive()) {
                endGame(msg().get("win.spouses-team"), msg().get("win.spouses-no-bad"));
                return true;
            }
            endGame(msg().get("win.good-team"), msg().get("win.good"));
            return true;
        }
        if (!goodAlive) {
            if (areSpousesAlive()) {
                endGame(msg().get("win.spouses-team"), msg().get("win.spouses-no-good"));
                return true;
            }
            endGame(msg().get("win.bad-team"), msg().get("win.bad"));
            return true;
        }
        return false;
    }

    private boolean areSpousesAlive() {
        if (spouses.isEmpty()) return false;
        for (Map.Entry<UUID, UUID> entry : spouses.entrySet()) {
            Player p1 = Bukkit.getPlayer(entry.getKey());
            Player p2 = Bukkit.getPlayer(entry.getValue());
            if (p1 != null && p2 != null) {
                GamePlayer gp1 = getGamePlayer(p1);
                GamePlayer gp2 = getGamePlayer(p2);
                if (gp1 != null && gp2 != null && gp1.isAlive() && gp2.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void endGame(String winningTeam, String reason) {
        phase = Phase.ENDED;
        cancelTask();
        removeBossBar();
        stopActionBar();
        stopParticleTask();
        broadcast(msg().get("game.game-over-header"));
        broadcast(msg().get("game.game-over-reason", MessageUtil.ph("reason", reason)));
        broadcast(msg().get("game.game-over-winner", MessageUtil.ph("winning_team", winningTeam)));

        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.sendMessage(plugin.prefix() + msg().get("game.your-role-was", MessageUtil.ph("role", gp.getRole().getName())));
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            Location lobby = plugin.getArenaManager().getGlobalLobby();
            if (lobby != null) {
                p.teleport(lobby);
            } else if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        revealAllRoles();
        players.clear();
        voteCounts.clear();
        sheriffElectionVotes.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        abilityCooldowns.clear();
        roleSelection.clear();
        fakeVoteCounts.clear();
        mapVotes.clear();
        sheriffId = null;
        mermaidFreezeUntil = 0;
        spouses.clear();
        cupidId = null;
        phase = Phase.LOBBY;
        scoreboardHelper.setupLobby();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!isPlayerInArena(online)) {
                addPlayer(online);
            }
        }
    }

    private void revealAllRoles() {
        broadcast(msg().get("game.role-reveal-header"));
        for (GamePlayer gp : players) {
            broadcast(msg().get("game.role-reveal-line", MessageUtil.ph("player", gp.getPlayer().getName(), "role", gp.getRole().getName())));
        }
    }

    public void forceStop() {
        cancelTask();
        removeBossBar();
        stopActionBar();
        stopParticleTask();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            Location lobby = plugin.getArenaManager().getGlobalLobby();
            if (lobby != null) {
                p.teleport(lobby);
            } else if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        players.clear();
        voteCounts.clear();
        sheriffElectionVotes.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        abilityCooldowns.clear();
        roleSelection.clear();
        fakeVoteCounts.clear();
        mapVotes.clear();
        sheriffId = null;
        mermaidFreezeUntil = 0;
        spouses.clear();
        cupidId = null;
        phase = Phase.LOBBY;
        dayCount = 0;
        scoreboardHelper.setupLobby();
        broadcast(msg().get("game.force-stopped"));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!isPlayerInArena(online)) {
                addPlayer(online);
            }
        }
    }

    private void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void setWorldTime(long time) {
        if (spawnLocation != null) {
            spawnLocation.getWorld().setTime(time);
        } else if (!players.isEmpty()) {
            players.iterator().next().getPlayer().getWorld().setTime(time);
        }
    }

    public void skipDay(Player player) {
        if (phase != Phase.DAY) {
            player.sendMessage(plugin.prefix() + msg().get("game.skip-day-not-day"));
            return;
        }
        int skipAmount = Math.max(1, phaseTimer / 3);
        phaseTimer -= skipAmount;
        broadcast(msg().get("game.skip-day-self", MessageUtil.ph("player", player.getName(), "seconds", String.valueOf(skipAmount), "timer", String.valueOf(Math.max(0, phaseTimer)))));
        player.getInventory().removeItem(ItemBuilder.create(plugin, "skip-day"));
        updateBossBar();
        if (phaseTimer <= 0) {
            cancelTask();
            taskId = -1;
            endDayPhase();
        }
    }

    public void skipDayFromCommand() {
        if (phase != Phase.DAY) return;
        phaseTimer = 0;
        broadcast(msg().get("game.skip-day-admin"));
        updateBossBar();
        cancelTask();
        taskId = -1;
        endDayPhase();
    }

    public void skipNightFromCommand() {
        if (phase != Phase.NIGHT) return;
        phaseTimer = 0;
        broadcast(msg().get("game.skip-night-admin"));
        updateBossBar();
        cancelTask();
        taskId = -1;
        endNightPhase();
    }

    public void skipElectionFromCommand() {
        if (phase != Phase.SHERIFF_ELECTION) return;
        phaseTimer = 0;
        broadcast(msg().get("game.skip-election-admin"));
        updateBossBar();
        cancelTask();
        taskId = -1;
        endSheriffElection();
    }

    public void forceSetRole(Player player, String roleName) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null) {
            return;
        }
        Role role;
        switch (roleName.toLowerCase()) {
            case "werewolf":
                role = new WerewolfRole();
                break;
            case "villager":
                role = new VillagerRole();
                break;
            case "witch":
                role = new WitchRole();
                break;
            case "seer":
                role = new SeerRole();
                break;
            case "hunter":
                role = new HunterRole();
                break;
            case "trickster":
                role = new TricksterRole();
                break;
            case "ninja":
                role = new NinjaRole();
                break;
            case "mermaid":
                role = new MermaidRole();
                break;
            case "masochist":
                role = new MasochistRole();
                break;
            case "cupid":
                role = new CupidRole();
                break;
            default:
                return;
        }
        gp.setRole(role);
        player.sendMessage(plugin.prefix() + msg().get("admin.setrole-notify", MessageUtil.ph("role", role.getName())));
        player.sendMessage(plugin.prefix() + msg().get("game.role-description", MessageUtil.ph("description", role.getDescription())));

        if (phase == Phase.SHERIFF_ELECTION) {
            player.getInventory().clear();
            player.getInventory().setItem(getItemSlot("vote-sheriff"), ItemBuilder.create(plugin, "vote-sheriff"));
            giveInfoItems(gp);
        } else if (phase == Phase.DAY) {
            player.getInventory().clear();
            if (!firstDay) {
                player.getInventory().setItem(getItemSlot("vote-sword"), ItemBuilder.create(plugin, "vote-sword"));
                player.getInventory().setItem(getItemSlot("revoke-vote"), ItemBuilder.create(plugin, "revoke-vote"));
            }
            player.getInventory().setItem(getItemSlot("skip-day"), ItemBuilder.create(plugin, "skip-day"));
            giveInfoItems(gp);
            role.onDayStart(player);
        } else if (phase == Phase.NIGHT) {
            player.getInventory().clear();
            role.onNightStart(player);
            giveInfoItems(gp);
        }
    }

    public void revealRolesToSender(CommandSender sender) {
        sender.sendMessage(msg().get("admin.reveal-header"));
        for (GamePlayer gp : players) {
            String status = gp.isAlive() ? msg().get("admin.reveal-alive") : msg().get("admin.reveal-dead");
            sender.sendMessage(msg().get("admin.reveal-line", MessageUtil.ph("player", gp.getPlayer().getName(), "status", status, "role", gp.getRole().getName())));
        }
    }

    public void sendSetupInfo(Player player) {
        int total = players.size();
        long werewolves = players.stream().filter(gp -> gp.getRole().isWerewolf()).count();
        long tricksters = players.stream().filter(gp -> gp.getRole().isTrickster()).count();
        long witches = players.stream().filter(gp -> gp.getRole() instanceof WitchRole).count();
        long seers = players.stream().filter(gp -> gp.getRole() instanceof SeerRole).count();
        long hunters = players.stream().filter(gp -> gp.getRole() instanceof HunterRole).count();
        long villagers = players.stream().filter(gp -> gp.getRole() instanceof VillagerRole).count();
        long ninjas = players.stream().filter(gp -> gp.getRole().isNinja()).count();
        long mermaids = players.stream().filter(gp -> gp.getRole().isMermaid()).count();
        long masochists = players.stream().filter(gp -> gp.getRole().isMasochist()).count();
        long cupids = players.stream().filter(gp -> gp.getRole().isCupid()).count();

        int dayDur = plugin.getConfig().getInt("day-duration", 120);
        int nightDur = plugin.getConfig().getInt("night-duration", 60);

        player.sendMessage(plugin.prefix() + msg().get("game.setup-header"));
        if (werewolves > 0) player.sendMessage(msg().get("game.setup-werewolves", MessageUtil.ph("count", String.valueOf(werewolves))));
        if (tricksters > 0) player.sendMessage(msg().get("game.setup-tricksters", MessageUtil.ph("count", String.valueOf(tricksters))));
        if (witches > 0) player.sendMessage(msg().get("game.setup-witches", MessageUtil.ph("count", String.valueOf(witches))));
        if (seers > 0) player.sendMessage(msg().get("game.setup-seers", MessageUtil.ph("count", String.valueOf(seers))));
        if (hunters > 0) player.sendMessage(msg().get("game.setup-hunters", MessageUtil.ph("count", String.valueOf(hunters))));
        if (villagers > 0) player.sendMessage(msg().get("game.setup-villagers", MessageUtil.ph("count", String.valueOf(villagers))));
        if (ninjas > 0) player.sendMessage(msg().get("game.setup-ninjas", MessageUtil.ph("count", String.valueOf(ninjas))));
        if (mermaids > 0) player.sendMessage(msg().get("game.setup-mermaids", MessageUtil.ph("count", String.valueOf(mermaids))));
        if (masochists > 0) player.sendMessage(msg().get("game.setup-masochists", MessageUtil.ph("count", String.valueOf(masochists))));
        if (cupids > 0) player.sendMessage(msg().get("game.setup-cupids", MessageUtil.ph("count", String.valueOf(cupids))));
        player.sendMessage(msg().get("game.setup-total", MessageUtil.ph("count", String.valueOf(total))));
        player.sendMessage(msg().get("game.setup-day-dur", MessageUtil.ph("seconds", String.valueOf(dayDur))));
        player.sendMessage(msg().get("game.setup-night-dur", MessageUtil.ph("seconds", String.valueOf(nightDur))));
    }

    public void sendWolfTeamInfo(Player player) {
        player.sendMessage(plugin.prefix() + msg().get("game.wolf-team-header"));
        boolean any = false;
        for (GamePlayer gp : players) {
            if (gp.getRole().isWerewolf() || gp.getRole().isTrickster()) {
                any = true;
                String status = gp.isAlive() ? msg().get("game.wolf-team-alive") : msg().get("game.wolf-team-dead");
                player.sendMessage(msg().get("game.wolf-team-line", MessageUtil.ph("player", gp.getPlayer().getName(), "status", status)));
            }
        }
        if (!any) {
            player.sendMessage(msg().get("game.wolf-team-none"));
        }
    }

    private void createBossBar(String title, BarColor color) {
        removeBossBar();
        bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        for (GamePlayer gp : players) {
            bossBar.addPlayer(gp.getPlayer());
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        int totalDuration;
        String phaseName;
        if (phase == Phase.SHERIFF_ELECTION) {
            totalDuration = electionDuration;
            phaseName = msg().get("game.boss-bar-election");
        } else if (phase == Phase.DAY) {
            totalDuration = dayDuration;
            phaseName = msg().get("game.boss-bar-day");
        } else if (phase == Phase.NIGHT) {
            totalDuration = nightDuration;
            phaseName = msg().get("game.boss-bar-night");
        } else {
            return;
        }
        if (totalDuration <= 0) return;
        double progress = Math.max(0.0, Math.min(1.0, (double) phaseTimer / (double) totalDuration));
        bossBar.setProgress(progress);
        bossBar.setTitle(phaseName + msg().get("game.boss-bar-suffix", MessageUtil.ph("timer", String.valueOf(Math.max(0, phaseTimer)))));
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void startActionBar() {
        stopActionBar();
        actionBarTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phase != Phase.DAY && phase != Phase.NIGHT && phase != Phase.SHERIFF_ELECTION) {
                    cancel();
                    actionBarTaskId = -1;
                    return;
                }
                for (GamePlayer gp : players) {
                    Player p = gp.getPlayer();
                    if (!p.isOnline()) continue;
                    String text;
                    if (phase == Phase.SHERIFF_ELECTION) {
                        text = msg().get("game.action-bar-election", MessageUtil.ph("timer", String.valueOf(Math.max(0, phaseTimer))));
                    } else if (gp.isAlive()) {
                        String phaseLabel = phase == Phase.DAY ? msg().raw("scoreboard.game.phase-day") : msg().raw("scoreboard.game.phase-night");
                        String sheriffText = gp.isSheriff() ? msg().get("game.action-bar-sheriff") : msg().get("game.action-bar-alive-text");
                        text = msg().get("game.action-bar-alive", MessageUtil.ph("role", gp.getRole().getName(), "phase", phaseLabel, "sheriff", sheriffText));
                    } else {
                        String phaseLabel = phase == Phase.DAY ? msg().raw("scoreboard.game.phase-day") : msg().raw("scoreboard.game.phase-night");
                        text = msg().get("game.action-bar-dead", MessageUtil.ph("phase", phaseLabel));
                    }
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
                }
            }
        }.runTaskTimer(plugin, 10L, 20L).getTaskId();
    }

    private void stopActionBar() {
        if (actionBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(actionBarTaskId);
            actionBarTaskId = -1;
        }
    }

    public void ninjaSelectAbility(Player player, String ability) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        NinjaRole ninjaRole = gp.asNinja();
        if (ninjaRole == null) return;
        if (ninjaRole.hasUsedAbilityTonight()) {
            player.sendMessage(plugin.prefix() + msg().get("game.ninja-already-used"));
            return;
        }
        ninjaRole.setSelectedAbility(ability);
        player.getInventory().setItem(getItemSlot("ninja-ability"), ItemBuilder.create(plugin, "ninja-ability"));
        String abilityName = ability.substring(0, 1).toUpperCase() + ability.substring(1);
        player.sendMessage(plugin.prefix() + msg().get("game.ninja-selected", MessageUtil.ph("ability", abilityName)));
    }

    public void ninjaExecuteAbility(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        NinjaRole ninjaRole = gp.asNinja();
        if (ninjaRole == null) return;
        String cooldownKey = player.getUniqueId() + ":ninja";
        if (isOnCooldown(cooldownKey, ninjaCooldown, player)) return;
        if (ninjaRole.hasUsedAbilityTonight()) {
            player.sendMessage(plugin.prefix() + msg().get("game.ninja-already-used"));
            return;
        }
        String ability = ninjaRole.getSelectedAbility();
        if (ability == null) {
            player.sendMessage(plugin.prefix() + msg().get("game.ninja-no-ability"));
            return;
        }
        setCooldown(cooldownKey);
        ninjaRole.setAbilityUsedTonight(true);
        player.getInventory().removeItem(ItemBuilder.create(plugin, "ninja-book"));
        player.getInventory().removeItem(ItemBuilder.create(plugin, "ninja-ability"));

        int durationTicks = 160;

        switch (ability) {
            case "vanish":
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false));
                particleTrailPlayers.add(player.getUniqueId());
                player.sendMessage(plugin.prefix() + msg().get("game.ninja-vanish"));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        particleTrailPlayers.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            case "sprint":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 4, false, false));
                player.sendMessage(plugin.prefix() + msg().get("game.ninja-sprint"));
                break;
            case "decoy":
                Location loc = player.getLocation();
                ArmorStand decoy = loc.getWorld().spawn(loc, ArmorStand.class);
                decoy.setVisible(false);
                decoy.setCustomName(player.getName());
                decoy.setCustomNameVisible(true);
                decoy.setGravity(false);
                decoy.setMarker(true);
                player.sendMessage(plugin.prefix() + msg().get("game.ninja-decoy"));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (decoy != null && !decoy.isDead()) {
                            decoy.remove();
                        }
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            case "disguise":
                ItemStack fakeHelmet = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_HELMET));
                ItemStack fakeChest = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_CHESTPLATE));
                ItemStack fakeLegs = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_LEGGINGS));
                ItemStack fakeBoots = ItemBuilder.makeUnbreakable(new ItemStack(Material.NETHERITE_BOOTS));
                PlayerInventory inv = player.getInventory();
                ItemStack oldHelmet = inv.getHelmet();
                ItemStack oldChest = inv.getChestplate();
                ItemStack oldLegs = inv.getLeggings();
                ItemStack oldBoots = inv.getBoots();
                inv.setHelmet(fakeHelmet);
                inv.setChestplate(fakeChest);
                inv.setLeggings(fakeLegs);
                inv.setBoots(fakeBoots);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false));
                particleTrailPlayers.add(player.getUniqueId());
                player.sendMessage(plugin.prefix() + msg().get("game.ninja-disguise"));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            inv.setHelmet(oldHelmet);
                            inv.setChestplate(oldChest);
                            inv.setLeggings(oldLegs);
                            inv.setBoots(oldBoots);
                        }
                        particleTrailPlayers.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            default:
                break;
        }
    }

    public void mermaidSing(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        MermaidRole mermaidRole = gp.asMermaid();
        if (mermaidRole == null) return;
        if (mermaidRole.hasSungTonight()) {
            player.sendMessage(plugin.prefix() + msg().get("game.mermaid-already-sung"));
            return;
        }
        mermaidRole.setSungTonight(true);
        mermaidFreezeUntil = System.currentTimeMillis() + (mermaidFreezeDuration * 1000L);
        player.getInventory().removeItem(ItemBuilder.create(plugin, "mermaid-shell"));
        player.sendMessage(plugin.prefix() + msg().get("game.mermaid-sing-self", MessageUtil.ph("seconds", String.valueOf(mermaidFreezeDuration))));
        broadcast(msg().get("game.mermaid-sing-broadcast"));
        for (GamePlayer wgp : players) {
            if (wgp.isAlive() && wgp.getRole().isWerewolf()) {
                wgp.getPlayer().sendMessage(plugin.prefix() + msg().get("game.mermaid-frozen", MessageUtil.ph("seconds", String.valueOf(mermaidFreezeDuration))));
            }
        }
    }

    public boolean isMermaidFreezeActive() {
        return mermaidFreezeUntil > 0 && System.currentTimeMillis() < mermaidFreezeUntil;
    }

    private boolean isOnCooldown(String key, int cooldownSeconds, Player player) {
        Long lastUsed = abilityCooldowns.get(key);
        if (lastUsed == null) return false;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        if (remaining > 0) {
            player.sendMessage(plugin.prefix() + msg().get("game.cooldown", MessageUtil.ph("seconds", String.valueOf(remaining))));
            return true;
        }
        return false;
    }

    private void setCooldown(String key) {
        abilityCooldowns.put(key, System.currentTimeMillis());
    }

    private void startParticleTask() {
        particleTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : particleTrailPlayers) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        Location loc = p.getLocation().add(0, 1, 0);
                        p.getWorld().spawnParticle(Particle.REDSTONE, loc, 5, 0.3, 0.6, 0.3, new Particle.DustOptions(Color.RED, 1.0f));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
    }

    private void stopParticleTask() {
        if (particleTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
        particleTrailPlayers.clear();
    }

    public void broadcast(String message) {
        String prefixed = ColorUtil.color(message);
        for (GamePlayer gp : players) {
            gp.getPlayer().sendMessage(prefixed);
        }
    }

}
