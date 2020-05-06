package de.codingair.warpsystem.spigot.features.teleportcommand.listeners;

import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.features.teleportcommand.TeleportCommandManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if(WarpSystem.hasPermission(e.getEntity(), WarpSystem.PERMISSION_USE_TELEPORT_COMMAND_BACK_DETECT_DEATHS))
            TeleportCommandManager.getInstance().addToBackHistory(e.getEntity(), e.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent e) {
        if(e.isCancelled() || !WarpSystem.hasPermission(e.getPlayer(), WarpSystem.PERMISSION_USE_TELEPORT_COMMAND_BACK)) return;

        if(e.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN && e.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) return;
        if(TeleportCommandManager.getInstance().getBackHistorySize() > 1 && TeleportCommandManager.getInstance().usingBackCommand(e.getPlayer())) return;
        TeleportCommandManager.getInstance().addToBackHistory(e.getPlayer(), e.getFrom());
    }

}
