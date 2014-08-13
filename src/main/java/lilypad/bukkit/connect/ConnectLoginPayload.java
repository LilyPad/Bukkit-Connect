package lilypad.bukkit.connect;

import com.google.gson.Gson;

public class ConnectLoginPayload {

	private static final Gson gson = new Gson();
	
	public static ConnectLoginPayload decode(String string) throws Exception {
		return gson.fromJson(string, ConnectLoginPayload.class);
	}
	
	public static String encode(ConnectLoginPayload payload) throws Exception {
		return gson.toJson(payload);
	}
	
	public static class Property {
		
		public String n;
		public String v;
		public String s;
		
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
	
	public String s;
	public String h;
	public String rIp;
	public int rP;
	public String n;
	public String u;
	public Property[] p;
	
	public ConnectLoginPayload() {
		// empty
	}
	
	public ConnectLoginPayload(String securityKey, String host, String realIp, int realPort, String name, String uuid, Property[] properties) {
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
	
	public String getUUID() {
		return this.u;
	}
	
	public Property[] getProperties() {
		return this.p;
	}
	
}
