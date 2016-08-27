package lilypad.bukkit.connect.login;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class LoginListener implements Listener {

	private ConnectPlugin connectPlugin;
	private LoginPayloadCache payloadCache;
	private SimpleDateFormat vanillaBanFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");

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
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		LoginPayload payload = payloadCache.getByName(player.getName());
		if (payload == null) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "LilyPad: Internal server error");
			return;
		}
		// Emulate a normal login procedure
		if (player.isBanned()) {
			BanList banList = this.connectPlugin.getServer().getBanList(BanList.Type.NAME);
			BanEntry banEntry = banList.getBanEntry(player.getName());

			StringBuilder banMessage = new StringBuilder();
			banMessage.append("You are banned from this server!\nReason: ");
			banMessage.append(banEntry.getReason());

			if (banEntry.getExpiration() != null) {
				if (banEntry.getExpiration().compareTo(new Date()) > 0) {
					banMessage.append("\nYour ban will be removed on " + vanillaBanFormat.format(banEntry.getExpiration()));
					event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage.toString());
					return;
				}
				// If the expiration is not null, but we got to here, it means that the ban has expired.
			} else {
				event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage.toString());
			}
		} else if (this.connectPlugin.getServer().hasWhitelist() && !player.isWhitelisted()) {
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, this.connectPlugin.getSpigotHook().isSpigot() ? this.connectPlugin.getSpigotHook().getWhitelistMessage() : "You are not white-listed on this server!");
		} else if (this.connectPlugin.getServer().getIPBans().contains(payload.getRealIp())) {
			BanList banList = this.connectPlugin.getServer().getBanList(BanList.Type.IP);
			BanEntry banEntry = banList.getBanEntry(payload.getRealIp());

			StringBuilder banMessage = new StringBuilder();
			banMessage.append("Your IP address is banned from this server!\nReason: ");
			banMessage.append(banEntry.getReason());

			if (banEntry.getExpiration() != null) {
				if (banEntry.getExpiration().compareTo(new Date()) > 0) {
					banMessage.append("\nYour ban will be removed on " + vanillaBanFormat.format(banEntry.getExpiration()));
					event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage.toString());
					return;
				}
				// If the expiration is not null, but we got to here, it means that the ban has expired.
			} else {
				event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage.toString());
			}
		} else if (sizeOf(this.connectPlugin.getServer().getOnlinePlayers()) >= this.connectPlugin.getServer().getMaxPlayers()) {
			event.disallow(PlayerLoginEvent.Result.KICK_FULL, this.connectPlugin.getSpigotHook().isSpigot() ? this.connectPlugin.getSpigotHook().getServerFullMessage() : "The server is full!");
		} else if (event.getResult() != PlayerLoginEvent.Result.KICK_OTHER) {
			event.allow();
		}
	}
	
	public int sizeOf(Object list) {
		if (list instanceof List) {
			return ((List<?>) list).size();
		}
		if (list instanceof Player[]) {
			return ((Player[]) list).length;
		}
		return 0;
	}

}
