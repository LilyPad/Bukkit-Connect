package lilypad.bukkit.connect.login;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class LoginListener implements Listener {

	private ConnectPlugin connectPlugin;
	private LoginPayloadCache payloadCache;

	public LoginListener(ConnectPlugin connectPlugin, LoginPayloadCache payloadCache) {
		this.connectPlugin = connectPlugin;
		this.payloadCache = payloadCache;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		LoginPayload payload = payloadCache.getByName(event.getName());
		if (payload == null) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "LilyPad: Internal server error");
            return;
		}
		// Store uuid
		UUID uuid = UUID.fromString(payload.getUUID().substring(0, 8) + "-" + payload.getUUID().substring(8, 12) + "-" + payload.getUUID().substring(12, 16) + "-" + payload.getUUID().substring(16, 20) + "-" + payload.getUUID().substring(20, 32));
		try {
			ReflectionUtils.setFinalField(AsyncPlayerPreLoginEvent.class, event, "uniqueId", uuid);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		LoginPayload payload = payloadCache.getByName(player.getName());
		// Store uuid
		UUID uuid = UUID.fromString(payload.getUUID().substring(0, 8) + "-" + payload.getUUID().substring(8, 12) + "-" + payload.getUUID().substring(12, 16) + "-" + payload.getUUID().substring(16, 20) + "-" + payload.getUUID().substring(20, 32));
		try {
			// Entity
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			Object gameProfile = ReflectionUtils.getPrivateField(entityPlayer.getClass().getSuperclass(), entityPlayer, Object.class, "bH");
			ReflectionUtils.setFinalField(gameProfile.getClass(), gameProfile, "id", uuid);
			ReflectionUtils.setFinalField(entityPlayer.getClass().getSuperclass().getSuperclass().getSuperclass(), entityPlayer, "uniqueID", uuid);
			// User cache
			Object craftServer = this.connectPlugin.getServer();
			Object minecraftServer = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "console");
			Method getUserCacheMethod = minecraftServer.getClass().getMethod("getUserCache");
			Object userCache = getUserCacheMethod.invoke(minecraftServer);
			Method cacheProfileMethod = userCache.getClass().getMethod("a", gameProfile.getClass());
			cacheProfileMethod.invoke(userCache, gameProfile);
			// Properties
			Method getPropertiesMethod = gameProfile.getClass().getMethod("getProperties");
			Object gameProfileProperties = getPropertiesMethod.invoke(gameProfile);
			Method gameProfilePropertiesClear = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("clear");
			Method gameProfilePropertiesPut = gameProfileProperties.getClass().getSuperclass().getDeclaredMethod("put", Object.class, Object.class);
			gameProfilePropertiesClear.invoke(gameProfileProperties);
			Constructor<?> propertyConstructor = Class.forName("com.mojang.authlib.properties.Property").getConstructor(String.class, String.class, String.class);
			for(int i = 0; i < payload.getProperties().length; i++) {
				String name = payload.getProperties()[i].getName();
				gameProfilePropertiesPut.invoke(gameProfileProperties, name, propertyConstructor.newInstance(name, payload.getProperties()[i].getValue(), payload.getProperties()[i].getSignature()));
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			return;
		}
		// Emulate a normal login procedure
		if (player.isBanned()) {
			// TODO reason and expiration? Is this possible?
			event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "You are banned from this server!");
		} else if (this.connectPlugin.getServer().hasWhitelist() && !player.isWhitelisted()) {
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "You are not white-listed on this server!");
		} else if (this.connectPlugin.getServer().getIPBans().contains(payload.getRealIp())) {
			// TODO reason and expiration? Is this possible?
			event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "Your IP address is banned from this server!");
		} else if (this.connectPlugin.getServer().getOnlinePlayers().size() >= this.connectPlugin.getServer().getMaxPlayers()) {
			event.disallow(PlayerLoginEvent.Result.KICK_FULL, "The server is full!");
		} else if (event.getResult() != PlayerLoginEvent.Result.KICK_OTHER) {
			event.allow();
		}
	}

}
