package lilypad.bukkit.connect;

import com.destroystokyo.paper.event.player.PlayerHandshakeEvent;
import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Slf4j
@RequiredArgsConstructor
public class PlayerHandshakeListener implements Listener {

    @NonNull
    private final ConnectPlugin plugin;
    private final Gson gson = new Gson();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHandshake(PlayerHandshakeEvent event) {
        try {
            // for some reason, the event starts off cancelled if bungeecord support is not enabled.
            // so, we need to mark the event as not cancelled before we do our logic.
            event.setCancelled(false);

            final String rawPayload = event.getOriginalHandshake();
            final LoginPayload payload;
            try {
                payload = LoginPayload.decode(rawPayload);
            } catch (Throwable throwable) {
                log.error("Failed to decode payload: " + rawPayload, throwable);
                event.setFailMessage("Failed to decide proxy payload");
                event.setFailed(true);
                return;
            }

            final String payloadSecurityKey = payload.getSecurityKey();
            if (payloadSecurityKey == null || !payloadSecurityKey.equalsIgnoreCase(plugin.getSecurityKey())) {
                log.error("Payload security key did not match plugin, payload is: " + rawPayload);
                event.setFailMessage("Failed to verify security key");
                event.setFailed(true);
                return;
            }

            event.setServerHostname(payload.getHost());
            event.setSocketAddressHostname(payload.getRealIp());
            event.setUniqueId(payload.getUUID());

            final LoginPayload.Property[] payloadProperties = payload.getProperties();
            final Property[] mojangProperties = new Property[payloadProperties.length];
            for (int i = 0; i < payloadProperties.length; i++) {
                final LoginPayload.Property payloadProperty = payloadProperties[i];
                mojangProperties[i] = new Property(payloadProperty.getName(), payloadProperty.getValue(), payloadProperty.getSignature());
            }
            event.setPropertiesJson(gson.toJson(mojangProperties));
        } catch (Throwable throwable) {
            // this is just for security. if theres some sort of error we don't know about,
            // let's prevent the possibility of a bad connection from occurring.
            log.error("Failed to rewrite handshake", throwable);
            event.setFailMessage("Internal LilyPad error");
            event.setFailed(true);
        }
    }

}
