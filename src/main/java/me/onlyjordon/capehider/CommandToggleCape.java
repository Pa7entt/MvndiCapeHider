package me.onlyjordon.capehider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private final CapeHider plugin;

    public CommandToggleCape(CapeHider plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("capehider.togglecape.self")) return false;
            if (!(sender instanceof Player player)) {
                sender.sendMessage(PLAYERS_ONLY);
                return true;
            }
            boolean nowVisible = plugin.toggleCape(player);
            sender.sendMessage(Component.text("Your cape is now " + (nowVisible ? "visible" : "hidden") + ".",
                    nowVisible ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("capehider.togglecape.others")) return false;

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(PLAYER_NOT_FOUND);
            return true;
        }
        // Folia: profile updates must happen on the target's owning region
        // thread. The sender (player or console) may not be on that thread,
        // so run the toggle on the target's entity scheduler. Adventure's
        // sendMessage is thread-safe, so replying from inside is fine.
        target.getScheduler().run(plugin, task -> {
            boolean nowVisible = plugin.toggleCape(target);
            sender.sendMessage(Component.text(target.getName() + "'s cape is now "
                    + (nowVisible ? "visible" : "hidden") + ".",
                    nowVisible ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        }, () -> sender.sendMessage(PLAYER_NOT_FOUND));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission("capehider.togglecape.others")) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(player.getName());
            }
        }
        return result;
    }
}
