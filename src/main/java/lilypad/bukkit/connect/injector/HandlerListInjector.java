package lilypad.bukkit.connect.injector;

import lilypad.bukkit.connect.util.ReflectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class HandlerListInjector extends HandlerList {

    private final Plugin plugin;
    private final List<RegisteredListener> startListeners = new ArrayList<>();
    private final List<RegisteredListener> middleListeners = new ArrayList<>();
    private final List<RegisteredListener> endListeners = new ArrayList<>();

    private HandlerListInjector(Plugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public static void prioritize(Plugin plugin, Class<? extends Event> event) throws Exception {
        HandlerList handlerList = ReflectionUtils.getPrivateField(event, null, HandlerList.class, "handlers");
        HandlerListInjector injector = new HandlerListInjector(plugin);
        // move the handlerslots
        EnumMap<EventPriority, ArrayList<RegisteredListener>> handlerListHandlerSlots = ReflectionUtils.getPrivateField(HandlerList.class, handlerList, EnumMap.class, "handlerslots");
        EnumMap<EventPriority, ArrayList<RegisteredListener>> injectorHandlerSlots = ReflectionUtils.getPrivateField(HandlerList.class, injector, EnumMap.class, "handlerslots");
        injectorHandlerSlots.putAll(handlerListHandlerSlots);
        // remove old from allLists
        ArrayList<HandlerList> allLists = ReflectionUtils.getPrivateField(HandlerList.class, null, ArrayList.class, "allLists");
        allLists.remove(handlerList);
        // replace event handlers
        ReflectionUtils.setFinalField(event, null, "handlers", injector);
    }

    @Override
    public synchronized void bake() {
        super.bake();
        RegisteredListener[] handlers = super.getRegisteredListeners();
        // TODO we can speed this up greatly using arrays. It's not really necessary though, as this isn't a hot function
        for (RegisteredListener handler : handlers) {
            if (handler.getPlugin().equals(plugin)) {
                if (handler.getPriority().equals(EventPriority.LOWEST)) {
                    this.startListeners.add(handler);
                    continue;
                } else if (handler.getPriority().equals(EventPriority.MONITOR)) {
                    this.endListeners.add(handler);
                    continue;
                }
            }
            this.middleListeners.add(handler);
        }
        List<RegisteredListener> handlerList = new ArrayList<>(handlers.length);
        handlerList.addAll(this.startListeners);
        handlerList.addAll(this.middleListeners);
        handlerList.addAll(this.endListeners);
        handlerList.toArray(handlers);
    }

}
