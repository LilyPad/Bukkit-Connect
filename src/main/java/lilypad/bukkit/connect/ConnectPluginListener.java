package lilypad.bukkit.connect;

import java.lang.reflect.Constructor;
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
import org.bukkit.event.player.PlayerQuitEvent;

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
		ConnectLoginPayload payload;
		try {
			String payloadString = playerLoginEvent.getHostname();
			payloadString = payloadString.substring(0, payloadString.lastIndexOf(':'));
			payload = ConnectLoginPayload.decode(payloadString);
			if(payload == null) {
				throw new Exception();
			}
		} catch(Exception exception) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Error: Authentication to LilyPad failed");
			return;
		}
		
		// verify integrity
		if (!payload.getSecurityKey().equals(this.connectPlugin.getSecurityKey())) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Error: Authentication to LilyPad failed");
			return;
		}
		
		// store host
		try {
			ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "hostname", payload.getHost());
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player address in PlayerLoginEvent: " + exception.getMessage());
		}

		// store IP address
		InetSocketAddress playerAddress = new InetSocketAddress(payload.getRealIp(), payload.getRealPort());
		try {
			ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "address", playerAddress.getAddress());
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player address in PlayerLoginEvent: " + exception.getMessage());
		}
		this.playersToAddresses.put(playerLoginEvent.getPlayer(), playerAddress);

		// store uuid
		UUID uuid = UUID.fromString(payload.getUUID().substring(0, 8) + "-" + payload.getUUID().substring(8, 12) + "-" + payload.getUUID().substring(12, 16) + "-" + payload.getUUID().substring(16, 20) + "-" + payload.getUUID().substring(20, 32));
		try {
			// entity
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			Object gameProfile = ReflectionUtils.getPrivateField(entityPlayer.getClass().getSuperclass(), entityPlayer, Object.class, "i");
			ReflectionUtils.setFinalField(gameProfile.getClass(), gameProfile, "id", uuid);
			ReflectionUtils.setFinalField(entityPlayer.getClass().getSuperclass().getSuperclass().getSuperclass(), entityPlayer, "uniqueID", uuid);
			// user cache
			Object craftServer = this.connectPlugin.getServer();
			Object minecraftServer = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "console");
			Method getUserCacheMethod = minecraftServer.getClass().getMethod("getUserCache");
			Object userCache = getUserCacheMethod.invoke(minecraftServer);
			Method cacheProfileMethod = userCache.getClass().getMethod("a", gameProfile.getClass());
			cacheProfileMethod.invoke(userCache, gameProfile);
			// properties
			Method getPropertiesMethod = gameProfile.getClass().getMethod("getProperties");
			Object gameProfileProperties = getPropertiesMethod.invoke(gameProfile);
			Method gameProfilePropertiesClear = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("clear");
			Method gameProfilePropertiesPut = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("put", Object.class, Object.class);
			gameProfilePropertiesClear.invoke(gameProfileProperties);
			Constructor<?> propertyConstructor = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property").getConstructor(String.class, String.class, String.class);
			for(int i = 0; i < payload.getProperties().length; i++) {
				String name = payload.getProperties()[i].getName();
				gameProfilePropertiesPut.invoke(gameProfileProperties, name, propertyConstructor.newInstance(name, payload.getProperties()[i].getValue(), payload.getProperties()[i].getSignature()));
			}
		} catch(Exception exception) {
			System.out.println("[Connect] Failed to store player UUID in EntityPlayer: " + exception.getMessage());
		}

		// emulate a normal login procedure
		if (player.isBanned()) {
			// TODO reason and expiration? Is this possible?
			playerLoginEvent.disallow(Result.KICK_BANNED, "You are banned from this server!");
		} else if (this.connectPlugin.getServer().hasWhitelist() && !player.isWhitelisted()) {
			playerLoginEvent.disallow(Result.KICK_WHITELIST, "You are not white-listed on this server!");
		} else if (this.connectPlugin.getServer().getIPBans().contains(payload.getRealIp())) {
			// TODO reason and expiration? Is this possible?
			playerLoginEvent.disallow(Result.KICK_BANNED, "Your IP address is banned from this server!");
		} else if (this.connectPlugin.getServer().getOnlinePlayers().length >= this.connectPlugin.getServer().getMaxPlayers()) {
			playerLoginEvent.disallow(Result.KICK_FULL, "The server is full!");
		} else if (playerLoginEvent.getResult() != Result.KICK_OTHER) {
			playerLoginEvent.allow();
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
		this.playersToAddresses.remove(playerQuitEvent.getPlayer());
	}

}
