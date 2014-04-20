package lilypad.bukkit.connect;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lilypad.bukkit.connect.util.ReflectionUtils;
import lilypad.packet.common.util.BufferUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.MapMaker;

public class ConnectPluginListener implements Listener, PluginMessageListener {

	private ConnectPlugin connectPlugin;
	private Map<Player, InetSocketAddress> playersToAddresses = new MapMaker().weakKeys().makeMap();
	private Set<UUID> initializingPlayers = new HashSet<UUID>();

	public ConnectPluginListener(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}

	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent playerLoginEvent) {
		Player player = playerLoginEvent.getPlayer();

		// verify integrity
		String[] playerData = playerLoginEvent.getHostname().split("\\:")[0].split("\\;");
		if (playerData.length < 3 || !playerData[0].equals(this.connectPlugin.getSecurityKey())) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Error: Authentication to LilyPad failed");
			return;
		}

		// store IP address
		InetSocketAddress playerAddress = new InetSocketAddress(playerData[1], Integer.parseInt(playerData[2]));
		try {
			ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "address", playerAddress.getAddress());
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player address in PlayerLoginEvent: " + exception.getMessage());
		}
		this.playersToAddresses.put(playerLoginEvent.getPlayer(), playerAddress);

		// store unique ID (1.7.2)
		try {
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			Object gameProfile = ReflectionUtils.getPrivateField(entityPlayer.getClass().getSuperclass(), entityPlayer, Object.class, "i");
			if (playerData[3].length() == 32) {
				UUID uuid = UUID.fromString(playerData[3].substring(0, 8) + "-" + playerData[3].substring(8, 12) + "-" + playerData[3].substring(12, 16) + "-" + playerData[3].substring(16, 20) + "-" + playerData[3].substring(20, 32));
				ReflectionUtils.setFinalField(gameProfile.getClass(), gameProfile, "id", uuid);
				ReflectionUtils.setFinalField(entityPlayer.getClass().getSuperclass().getSuperclass().getSuperclass(), entityPlayer, "uniqueID", uuid);
			} else {
				System.out.println("[Connect] Unexpected UUID length: " + playerData[3].length());
			}
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player UUID in EntityPlayer: " + exception.getMessage());
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
		
		// invisibility inject
		try {
			Set<UUID> hiddenPlayersDelegate = (Set<UUID>) ReflectionUtils.getPrivateField(player.getClass(), player, Set.class, "hiddenPlayers");
			ReflectionUtils.setFinalField(player.getClass(), player, "hiddenPlayers", new HiddenPlayersSet(hiddenPlayersDelegate, this.initializingPlayers));
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to inject hiddenPlayers in CraftPlayer: " + exception.getMessage());
		}
		
		// invisibility
		this.initializingPlayers.add(player.getUniqueId());
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

			InetSocketAddress socketAddress = this.playersToAddresses.remove(player);
			try {
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "n", socketAddress);
			} catch(Exception exception1) {
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "l", socketAddress);
			}
		} catch (Exception exception) {
			System.out.println("[Connect] Failed to store player address in INetworkManager: " + exception.getMessage());
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
		Player player = playerQuitEvent.getPlayer();
		this.playersToAddresses.remove(player);
		this.initializingPlayers.remove(player.getUniqueId());
	}
	
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		try {
			ByteBuf buffer = Unpooled.wrappedBuffer(message);
			int length = BufferUtils.readVarInt(buffer);

			// game profile
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			Object gameProfile = ReflectionUtils.getPrivateField(entityPlayer.getClass().getSuperclass(), entityPlayer, Object.class, "i");

			// properties
			Method getPropertiesMethod = gameProfile.getClass().getMethod("getProperties");
			Object gameProfileProperties = getPropertiesMethod.invoke(gameProfile);
			Method gameProfilePropertiesClear = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("clear");
			Method gameProfilePropertiesPut = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("put", Object.class, Object.class);
			gameProfilePropertiesClear.invoke(gameProfileProperties);
			Constructor<?> propertyConstructor = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property").getConstructor(String.class, String.class, String.class);
			for(int i = 0; i < length; i++) {
				String name = BufferUtils.readString(buffer);
				gameProfilePropertiesPut.invoke(gameProfileProperties, name, propertyConstructor.newInstance(name, BufferUtils.readString(buffer), BufferUtils.readString(buffer)));
			}
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to alter game profile in EntityPlayer: " + exception.getMessage());
		}
		
		// invisibility
		this.initializingPlayers.remove(player.getUniqueId());
		for(Player otherPlayer : player.getServer().getOnlinePlayers()) {
			if(!otherPlayer.canSee(player)) {
				continue;
			}
			otherPlayer.hidePlayer(player);
			otherPlayer.showPlayer(player);
		}
		
	}

}
