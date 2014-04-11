package lilypad.bukkit.connect;

import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.common.collect.ForwardingMap;

public class HiddenPlayersMap extends ForwardingMap<String, Player> {

	private Map<String, Player> delegate;
	private Set<String> initializingPlayers;
	
	public HiddenPlayersMap(Map<String, Player> delegate, Set<String> initializingPlayers) {
		this.delegate = delegate;
		this.initializingPlayers = initializingPlayers;
	}
	
	public boolean containsKey(Object key) {
		if(this.initializingPlayers.contains(key)) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			if(stackTraceElements[1].getMethodName().equals("showPlayer") && !stackTraceElements[2].getClassName().equals("ConnectPluginListener")) {
				return false; // player is not invisible - this way we can avoid showing the player
			} else if(stackTraceElements[1].getMethodName().equals("canSee") && stackTraceElements[2].getClassName().equals("ConnectPluginListener")) {
				return super.containsKey(key); // return the actual key
			}
			return true; // player is invisible
		}
		return super.containsKey(key);
	}
	
	protected Map<String, Player> delegate() {
		return this.delegate;
	}

}
