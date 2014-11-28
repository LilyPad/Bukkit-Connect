package lilypad.bukkit.connect.login;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class LoginPayloadCache {

	private Cache<String, LoginPayload> payloads = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.SECONDS).build();
	
	public void submit(LoginPayload payload) {
		this.payloads.put(payload.getName(), payload);
	}
	
	public LoginPayload getByName(String name) {
		return this.payloads.getIfPresent(name);
	}
	
}
