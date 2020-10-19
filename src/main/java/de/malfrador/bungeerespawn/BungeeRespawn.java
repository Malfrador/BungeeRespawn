package de.malfrador.bungeerespawn;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public final class BungeeRespawn extends JavaPlugin implements Listener, PluginMessageListener {

    Set<String> playersToRespawn = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onDeath(PlayerRespawnEvent event) {
        if (event.getPlayer().hasPermission("bungeeRespawn.exempt")) {
            return;
        }
        if (getConfig().getString("respawnServer") == null || getConfig().getString("respawnServer").equals("none") ) {
            return;
        }

        ByteArrayDataOutput sendPlayer = ByteStreams.newDataOutput();
        sendPlayer.writeUTF("Connect");
        sendPlayer.writeUTF(getConfig().getString("respawnServer"));
        event.getPlayer().sendPluginMessage(this, "BungeeCord", sendPlayer.toByteArray());
        ByteArrayDataOutput pluginmessage = ByteStreams.newDataOutput();

        pluginmessage.writeUTF("Forward");
        pluginmessage.writeUTF(getConfig().getString("respawnServer"));
        pluginmessage.writeUTF("BungeeRespawn");
        pluginmessage.writeUTF(event.getPlayer().getName());
        event.getPlayer().sendPluginMessage(this, "BungeeCord", pluginmessage.toByteArray());
        Bukkit.getLogger().log(Level.INFO, event.getPlayer().getName() + " is respawning on " + getConfig().getString("respawnServer"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        OfflinePlayer player = event.getPlayer();
        BukkitRunnable runLater = new BukkitRunnable() {
            @Override
            public void run() {
                if (!playersToRespawn.contains(player.getName())) {
                    return;
                }
                Location respawnLoc = new Location(Bukkit.getWorld(getConfig().getString("respawnLocation.world")), getConfig().getDouble("respawnLocation.x"), getConfig().getDouble("respawnLocation.y"), getConfig().getDouble("respawnLocation.z"));
                player.getPlayer().teleport(respawnLoc);
                playersToRespawn.remove(player.getName());
            }
        };
        runLater.runTaskLater(this, 20);
    }


    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        OfflinePlayer respawnPlayer = null;
        if (subchannel.equals("BungeeRespawn")) {
           respawnPlayer = Bukkit.getOfflinePlayer(in.readUTF());
        }
        if (respawnPlayer == null) {
            return;
        }
        playersToRespawn.add(respawnPlayer.getName());
    }

}
