package lilypad.bukkit.connect;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.UUID;

public class LoginPayload {

    private static final Gson gson = new Gson();

    public static LoginPayload decode(String string) throws JsonSyntaxException {
        final LoginPayload result = gson.fromJson(string, LoginPayload.class);
        if (result == null) {
            throw new IllegalStateException("Result is null");
        }
        return result;
    }

    private String s;
    private String h;
    private String rIp;
    private int rP;
    private String n;
    private String u;
    private Property[] p;

    public String getSecurityKey() {
        return s;
    }

    public String getHost() {
        return h;
    }

    public String getRealIp() {
        return rIp;
    }

    public int getRealPort() {
        return rP;
    }

    public String getName() {
        return n;
    }

    public UUID getUUID() {
        return UUID.fromString(u.substring(0, 8) + "-" + u.substring(8, 12) + "-" + u.substring(12, 16) + "-" + u.substring(16, 20) + "-" + u.substring(20, 32));
    }

    public Property[] getProperties() {
        return this.p;
    }

    public static class Property {

        private String n;
        private String v;
        private String s;

        public String getName() {
            return n;
        }

        public String getValue() {
            return v;
        }

        public String getSignature() {
            return s;
        }

    }

}
