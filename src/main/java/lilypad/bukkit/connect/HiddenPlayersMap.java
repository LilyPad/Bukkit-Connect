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
	
	@Override
	public boolean containsKey(Object key) {
		if(this.initializingPlayers.contains(key)) {
			return true; // player is invisible
		}
		return super.containsKey(key);
	}
	
	protected Map<String, Player> delegate() {
		return this.delegate;
	}

}
