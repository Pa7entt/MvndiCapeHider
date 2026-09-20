package me.onlyjordon.capehider;

import me.onlyjordon.nicknamingapi.Nicknamer;
import me.onlyjordon.nicknamingapi.NicknamerAPI;
import me.onlyjordon.nicknamingapi.events.PlayerSkinLayerChangeEvent;
import me.onlyjordon.nicknamingapi.utils.SkinLayers;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CapeHider extends JavaPlugin implements Listener {

    private Nicknamer nicknamer;

    @Override
    public void onEnable() {
        nicknamer = NicknamerAPI.getNicknamer();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("togglecape").setExecutor(new CommandToggleCape(nicknamer));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("capehider.bypass")) {
            nicknamer.setSkinLayerVisible(event.getPlayer(), SkinLayers.SkinLayer.CAPE, false);
        }
    }

    @EventHandler
    public void onSkinLayerChange(PlayerSkinLayerChangeEvent event) {
        if (event.getReason() == PlayerSkinLayerChangeEvent.Reason.PLUGIN) return;
        SkinLayers layers = event.getNewLayers();
        SkinLayers oldLayers = event.getPreviousLayers();
        if (layers == null) return;
        boolean enabled = oldLayers != null && oldLayers.isLayerVisible(SkinLayers.SkinLayer.CAPE);
        layers.setLayerVisible(SkinLayers.SkinLayer.CAPE, enabled);
        event.setNewLayers(layers);
    }
}
