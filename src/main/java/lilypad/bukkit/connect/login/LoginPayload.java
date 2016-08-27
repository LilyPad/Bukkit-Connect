package lilypad.bukkit.connect.login;

import com.google.gson.Gson;
import java.util.UUID;

public class LoginPayload {

	private static final Gson gson = new Gson();
	
	public static LoginPayload decode(String string) throws Exception {
		return gson.fromJson(string, LoginPayload.class);
	}
	
	public static String encode(LoginPayload payload) throws Exception {
		return gson.toJson(payload);
	}
	
	public static class Property {
		
		private String n;
		private String v;
		private String s;
		
		public Property() {
			// empty
		}
		
		public Property(String name, String value, String signature) {
			this.n = name;
			this.v = value;
			this.s = signature;
		}
		
		public String getName() {
			return this.n;
		}
		
		public String getValue() {
			return this.v;
		}
		
		public String getSignature() {
			return this.s;
		}
		
	}
	
	private String s;
	private String h;
	private String rIp;
	private int rP;
	private String n;
	private String u;
	private Property[] p;
	
	public LoginPayload() {
		// empty
	}
	
	public LoginPayload(String securityKey, String host, String realIp, int realPort, String name, String uuid, Property[] properties) {
		this.s = securityKey;
		this.h = host;
		this.rIp = realIp;
		this.rP = realPort;
		this.n = name;
		this.u = uuid;
		this.p = properties;
	}
	
	public String getSecurityKey() {
		return this.s;
	}
	
	public String getHost() {
		return this.h;
	}
	
	public String getRealIp() {
		return this.rIp;
	}
	
	public int getRealPort() {
		return this.rP;
	}
	
	public String getName() {
		return this.n;
	}
	
	public UUID getUUID() {
		return UUID.fromString(this.u.substring(0, 8) + "-" + this.u.substring(8, 12) + "-" + this.u.substring(12, 16) + "-" + this.u.substring(16, 20) + "-" + this.u.substring(20, 32));
	}
	
	public Property[] getProperties() {
		return this.p;
	}
	
}
