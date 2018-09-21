package lilypad.bukkit.connect.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CloudDisconnectEvent extends Event {

    private final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public CloudDisconnectEvent() {
        super(false);
    }
}
