package lilypad.bukkit.connect.login;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener implements Listener {

    private final LoginPayloadCache payloadCache;

    public LoginListener(LoginPayloadCache payloadCache) {
        this.payloadCache = payloadCache;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        LoginPayload payload = payloadCache.getByName(event.getName());
        if (payload == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "LilyPad: Internal server error");
        }
    }
}
