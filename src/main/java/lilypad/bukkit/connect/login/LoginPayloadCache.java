package lilypad.bukkit.connect.login;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.concurrent.TimeUnit;

public class LoginPayloadCache {

    private final Cache<String, LoginPayload> payloads = new Cache2kBuilder<String, LoginPayload>() {
    }.expireAfterWrite(30, TimeUnit.SECONDS).build();

    //private Cache<String, LoginPayload> payloads = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public void submit(LoginPayload payload) {
        this.payloads.put(payload.getName(), payload);
    }

    public LoginPayload getByName(String name) {
        return this.payloads.get(name);
    }

}
