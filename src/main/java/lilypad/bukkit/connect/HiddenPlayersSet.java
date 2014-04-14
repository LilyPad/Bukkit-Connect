package lilypad.bukkit.connect;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ForwardingSet;

public class HiddenPlayersSet extends ForwardingSet<UUID> {

	private Set<UUID> delegate;
	private Set<UUID> initializingPlayers;
	
	public HiddenPlayersSet(Set<UUID> delegate, Set<UUID> initializingPlayers) {
		this.delegate = delegate;
		this.initializingPlayers = initializingPlayers;
	}
	
	@Override
	public boolean contains(Object key) {
		if(this.initializingPlayers.contains(key)) {
			return true; // player is invisible
		}
		return super.contains(key);
	}
	
	protected Set<UUID> delegate() {
		return this.delegate;
	}

}
