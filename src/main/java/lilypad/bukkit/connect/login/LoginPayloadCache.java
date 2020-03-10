package lilypad.bukkit.connect.login;



import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;


public class LoginPayloadCache {

	private Cache<String, LoginPayload> payloads = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

	public void submit(LoginPayload payload) {
		this.payloads.put(payload.getName(), payload);
	}
	
	public LoginPayload getByName(String name) {
		return this.payloads.getIfPresent(name);
	}
	
}
