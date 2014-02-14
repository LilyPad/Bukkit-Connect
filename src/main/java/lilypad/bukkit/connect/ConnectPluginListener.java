package lilypad.bukkit.connect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.google.common.collect.MapMaker;

public class ConnectPluginListener implements Listener {

	private ConnectPlugin connectPlugin;
	private Map<Player, InetSocketAddress> playersToAddresses = new MapMaker().weakKeys().makeMap();

	public ConnectPluginListener(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent playerLoginEvent) {
		Player player = playerLoginEvent.getPlayer();

		// verify integrity
		String[] playerData = playerLoginEvent.getHostname().split("\\:")[0].split("\\;");
		if (playerData.length < 3) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Error: Authentication to LilyPad failed");
			return;
		}
		if (!playerData[0].equals(this.connectPlugin.getSecurityKey())) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Error: Authentication to LilyPad failed");
			return;
		}

		// store IP address
		InetSocketAddress playerAddress = new InetSocketAddress(playerData[1], Integer.parseInt(playerData[2]));
		try {
			ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "address", playerAddress.getAddress());
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player address in PlayerLoginEvent");
		}
		this.playersToAddresses.put(playerLoginEvent.getPlayer(), playerAddress);

		// store unique ID (1.7.2)
		try {
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			if (playerData[3].length() == 32) {
				ReflectionUtils.setFinalField(entityPlayer.getClass().getSuperclass().getSuperclass().getSuperclass(), entityPlayer, "uniqueID", UUID.fromString(playerData[3].substring(0, 8) + "-" + playerData[3].substring(8, 12) + "-" + playerData[3].substring(12, 16) + "-" + playerData[3].substring(16, 20) + "-" + playerData[3].substring(20, 32)));
			} else {
				System.out.println("[Connect] Unexpected UUID length: " + playerData[3].length());
			}
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player UUID in EntityPlayer");
		}

		// emulate a normal login procedure with the IP address
		if (playerLoginEvent.getResult() == Result.KICK_BANNED && playerLoginEvent.getKickMessage().startsWith("Your IP address is banned from this server!\nReason: ")) {
			if (this.connectPlugin.getServer().getIPBans().contains(playerData[1])) {
				playerLoginEvent.disallow(Result.KICK_BANNED, "Your IP address is banned from this server!");
			} else if (this.connectPlugin.getServer().getOnlinePlayers().length >= this.connectPlugin.getServer().getMaxPlayers()) {
				playerLoginEvent.disallow(Result.KICK_FULL, "The server is full!");
			} else {
				playerLoginEvent.allow();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
		Player player = playerJoinEvent.getPlayer();

		// store IP address
		try {
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);

			Field playerConnectionField = entityPlayer.getClass().getField("playerConnection");
			Object playerConnection = playerConnectionField.get(entityPlayer);

			Field networkManagerField = playerConnection.getClass().getField("networkManager");
			Object networkManager = networkManagerField.get(playerConnection);

			ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "l", this.playersToAddresses.remove(player));
		} catch (Exception exception) {
			System.out.println("[Connect] Failed to store player address in INetworkManager");
		}
	}

}
