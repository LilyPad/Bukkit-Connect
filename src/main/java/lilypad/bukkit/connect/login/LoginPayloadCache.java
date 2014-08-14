package lilypad.bukkit.connect.login;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.MapMaker;

public class LoginPayloadCache {

	@SuppressWarnings("deprecation") // TODO use stuff not deprecated
	private Map<String, LoginPayload> payloads = new MapMaker().expireAfterWrite(30, TimeUnit.SECONDS).makeMap();
	
	public void submit(LoginPayload payload) {
		this.payloads.put(payload.getName(), payload);
	}
	
	public LoginPayload removeByName(String name) {
		return this.payloads.remove(name);
	}
	
	public LoginPayload getByName(String name) {
		return this.payloads.get(name);
	}
	
}
