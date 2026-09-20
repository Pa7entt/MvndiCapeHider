package me.onlyjordon.capehider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.onlyjordon.nicknamingapi.Nicknamer;
import me.onlyjordon.nicknamingapi.utils.SkinLayers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CommandToggleCape implements CommandExecutor, TabCompleter {

    private static final Component PLAYERS_ONLY = Component.text("You must be a player to use this command.")
        .color(NamedTextColor.RED);
    private static final Component PLAYER_NOT_FOUND = Component.text("Player not found.")
        .color(NamedTextColor.RED);

    private final Nicknamer nicknamer;

    public CommandToggleCape(Nicknamer nicknamer) {
        this.nicknamer = nicknamer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("capehider.togglecape.self")) {
                return false;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(PLAYERS_ONLY);
                return true;
            }
            toggleCape(player);
            return true;
        }
        if (!sender.hasPermission("capehider.togglecape.others")) {
            return false;
        }
        Player target = nicknamer.getPlayerWithNick(args[0]);
        if (target == null) {
            sender.sendMessage(PLAYER_NOT_FOUND);
            return true;
        }
        toggleCape(target);
        return true;
    }

    private void toggleCape(Player player) {
        boolean nowVisible = !nicknamer.getVisibleSkinLayers(player).contains(SkinLayers.SkinLayer.CAPE);
        nicknamer.setSkinLayerVisible(player, SkinLayers.SkinLayer.CAPE, nowVisible);
        nicknamer.refreshPlayer(player);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission("capehider.togglecape.others")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String nick = nicknamer.getNick(player);
            if (nick == null) {
                nick = player.getName();
            }
            if (nick.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(nick);
            }
        }
        return result;
    }
}
